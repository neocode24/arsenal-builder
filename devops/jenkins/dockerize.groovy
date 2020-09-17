def label = "ArsenalDev-${UUID.randomUUID().toString()}"
def isIpcRunningEnv = true
def isEpcRunningEnv = false
def mattermostDevIncomingUrl=''

String getBranchName(branch) {
    branchTemp=sh returnStdout:true ,script:"""echo "$branch" |sed -E "s#origin[0-9]*/##g" """
    if(branchTemp){
        branchTemp=branchTemp.trim()
    }
    return branchTemp
}

podTemplate(label: label, serviceAccount: 'tiller', namespace: 'devops',
    containers: [
        containerTemplate(name: 'build-tools', image: 'registry.ktdscoe.myds.me:5500/arsenal/build-tools:latest', ttyEnabled: true, command: 'cat', privileged: true, alwaysPullImage: true)
    ],
    volumes: [
        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
        persistentVolumeClaim(mountPath: '/home/jenkins', claimName: 'jenkins-slave-pvc', readOnly: false)
        ]
    ) {

    node(label) {
        if ( isIpcRunningEnv ) {
            library 'pipeline-lib'
        }
        try {
            // freshStart 
            def freshStart = params.freshStart

            if ( freshStart ) {
                container('build-tools'){
                    // remove previous working dir
                    print "freshStart... clean working directory ${env.JOB_NAME}"
                    sh 'ls -A1|xargs rm -rf' /* clean up our workspace */
                }
            }

            
            def commitId

            def branchTemp
            //branch Name Parsing
            branchTemp = params.branchName
            branch=getBranchName(branchTemp)

            print "branch : " + branch
            
            stage('Get Source') {
                git url: "https://gitlab.com/arsenal-portal/arsenal2.0/arsenal-builder.git",
                    credentialsId: 'external-gitlab',
                    branch: "${branch}"
                    commitId = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            }

            def props = readProperties  file:'devops/jenkins/dockerize.properties'
            def tag = commitId
            def dockerRegistry = props['dockerRegistry']
            def image = props['image']
            def selector = props['selector']
            def namespace = props['namespace']
            def appname = props['appname']

            def unitTestEnable = true
            unitTestEnable = params.unitTestEnable
            
            def mvnSettings = "${env.WORKSPACE}/devops/jenkins/settings.xml"
            if ( isIpcRunningEnv ) {
                mvnSettings = "${env.WORKSPACE}/devops/jenkins/settings.xml"
            } else {
                mvnSettings = "${env.WORKSPACE}/devops/jenkins/settings-epc.xml"
            }
                     
            stage('gradle build project') {
                container('build-tools') {
                    if (unitTestEnable){
                        sh returnStdout:true, script:"gradle clean test"

                    } else {
                        sh "gradle clean bootJar"
                    }
                }
            }

            def pushRegistry = params.pushRegistry
            stage('Build Docker image') {
                container('build-tools') {
                    docker.withRegistry("${dockerRegistry}", 'cluster-registry-credentials') {

                        sh "docker build -t ${pushRegistry}/${image}:${tag} -f devops/jenkins/Dockerfile --build-arg sourceFile=`find build/libs -name '*.jar' | head -n 1` ."
                        sh "docker push ${pushRegistry}/${image}:${tag}"
                        sh "docker tag ${pushRegistry}/${image}:${tag} ${pushRegistry}/${image}:latest"
                        sh "docker push ${pushRegistry}/${image}:latest"

                        sh "docker rmi ${pushRegistry}/${image}:${tag}"
                        sh "docker rmi ${pushRegistry}/${image}:latest"
                    }
                }
            }
            
            stage( 'Helm lint' ) {
                container('build-tools') {
                    dir('devops/helm/arsenal-builder'){
                        if ( isIpcRunningEnv ) {
                            sh """
                            # initial helm
                            # central helm repo can't connect
                            # setting stable repo by local repo
                            helm init --client-only --stable-repo-url "http://127.0.0.1:8879/charts" --skip-refresh
                            helm lint --namespace devops --tiller-namespace devops .
                            """
                        } else {
                            sh """
                            helm lint --namespace devops .
                            """
                        }
                  }
                }
            }
            
            stage("Summary") {
                if ( mattermostDevIncomingUrl && isIpcRunningEnv ) {
                   gl_SummaryMessageMD(mattermostDevIncomingUrl)
                }
            }
        } catch(e) {
            container('build-tools'){
                print "Clean up ${env.JOB_NAME} workspace..."
                sh 'ls -A1|xargs rm -rf' /* clean up our workspace */
            }

            currentBuild.result = "FAILED"
            if ( mattermostDevIncomingUrl && isIpcRunningEnv ) {
               def buildMessage="**Error "+ e.toString()+"**"
               gl_SummaryMessageMD(mattermostDevIncomingUrl, 'F', buildMessage)
            } else {
                print " **Error :: " + e.toString()+"**"
            }

        }
    }
}
