def label = "ArsenalDev-${UUID.randomUUID().toString()}"
def isIpcRunningEnv = true
def isEpcRunningEnv = false
def mattermostDevIncomingUrl=''

String getBranchName(branch) {
    branchTemp=sh returnStdout:true ,script:"""echo "$branch" |sed -E "s#origin/##g" """
    if(branchTemp){
        branchTemp=branchTemp.trim()
    }
    return branchTemp
}

podTemplate(label: label, serviceAccount: 'tiller', namespace: 'devops',
    containers: [
        containerTemplate(name: 'build-tools', image: 'registry.ktdscoe.myds.me:5500/arsenal/build-tools:latest', ttyEnabled: true, command: 'cat', privileged: true, alwaysPullImage: true),
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

            def branchTemp
            //branch Name Parsing
            branchTemp = params.branchName            
            branchName=getBranchName(branchTemp)
            
            print "branchName = " + branchName

            def milestone
            milestone=params.version
            def milestoneId

            stage('Get Source') {

                sh """
                    git config --global user.email "jenkins@kt.com"
                    git config --global user.name "jenkins"
                    git config --global credential.helper cache
                    git config --global push.default simple
                """
                git url: "https://gitlab.com/arsenal-portal/arsenal2.0/arsenal-builder.git",
                    credentialsId: 'external-gitlab',
                    branch: "${branchName}"
            
            }

            def props = readProperties  file:'devops/jenkins/tag.properties'
            def dockerRegistry = props['dockerRegistry']
            def image = props['image']
            def gitProjectUrl = props['gitProjectUrl']

            stage('Check Milestone'){
                withCredentials([string(credentialsId: 'external-gitlab-token', variable: 'TOKEN')]){            
                    container('build-tools'){
                        result = sh returnStdout:true, script: """curl --header PRIVATE-TOKEN:$TOKEN ${gitProjectUrl}/milestones?state=active | jq -r '.[] | select(.title == "${milestone}") | .id'"""
                        try {
                            result = result.toInteger()
                        } catch (e) {
                            result = 0
                        }
                        
                        if ( result <= 0 ){
                           autoCancelled = true
                           print "Error MileStone(version) is not opend"
                           error('Error MileStone(version) is not opend')
                        }
                        
                        milestoneId = result
                     }
                 }
            }
        
            stage('Check Tag'){
                withCredentials([string(credentialsId: 'external-gitlab-token', variable: 'TOKEN')]){
                    container('build-tools'){
                        result = sh returnStdout:true, script: """curl --header PRIVATE-TOKEN:$TOKEN ${gitProjectUrl}/repository/tags | jq -r '.[].name' | grep -w '^${milestone}\$' | wc -l"""
                        result = result.toInteger()
                    
                        if ( result != 0 ){
                            autoCancelled = true
                            print "Error Already builded Tag is exist"
                            error('Error Already builded Tag is exist')
                        }
                     }
                }
            }

            def mvnSettings = "${env.WORKSPACE}/devops/jenkins/settings.xml"
            if ( isIpcRunningEnv ) {
                mvnSettings = "${env.WORKSPACE}/devops/jenkins/settings.xml"
            } else {
                mvnSettings = "${env.WORKSPACE}/devops/jenkins/settings-epc.xml"
            }
                     
            stage('gradle build project') {
                container('build-tools') {
                    sh "gradle clean bootJar"
                }
            }

            def pushRegistry = params.pushRegistry
            stage('Build Docker image') {
                container('build-tools') {
                    docker.withRegistry("${dockerRegistry}", 'cluster-registry-credentials') {

                        sh "docker build -t ${pushRegistry}/${image}:${milestone} -f devops/jenkins/Dockerfile --build-arg sourceFile=`find build/libs -name '*.jar' | head -n 1` ."
                        sh "docker push ${pushRegistry}/${image}:${milestone}"

                        sh "docker tag ${pushRegistry}/${image}:${milestone} docker-registry.default.svc:5000/${image}:latest"
                        sh "docker push docker-registry.default.svc:5000/${image}:latest"

                        sh "docker tag ${pushRegistry}/${image}:${milestone} docker-registry.default.svc:5000/${image}:${milestone}"
                        sh "docker push docker-registry.default.svc:5000/${image}:${milestone}"

                        sh "docker tag ${pushRegistry}/${image}:${milestone} ${pushRegistry}/${image}:latest"
                        sh "docker push ${pushRegistry}/${image}:latest"
                        sh "docker rmi ${pushRegistry}/${image}:${milestone}"
                        sh "docker rmi ${pushRegistry}/${image}:latest"
                    }
                }
            }

            stage('tagging Version') {

                try {
                    withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: 'external-gitlab', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']
                        ]) {
                        sh("git config credential.username ${env.GIT_USERNAME}")
                        sh("git config credential.helper '!echo password=\$GIT_PASSWORD; echo'")
                        sh("GIT_ASKPASS=true git push origin --tags")
                    
                        sh """
                            sed -i "s/^version.*/version: ${milestone}/g" devops/helm/arsenal-builder/values.yaml

                            git commit -am "helm version changed"
                            git tag -a ${milestone} -m "jenkins added"
                            git push --set-upstream origin ${branchName} 
                            git push --tags
                        """
                    }
                } finally {
                    sh("git config --unset credential.username")
                    sh("git config --unset credential.helper")
                }
                
            }
            
            stage('Milestone Close') {
                
                // delete release branch & milestone close.
                try {
                    withCredentials([string(credentialsId: 'external-gitlab-token', variable: 'TOKEN')]){
                        container('build-tools'){
                            sh("curl --header PRIVATE-TOKEN:$TOKEN --request DELETE ${gitProjectUrl}/repository/branches/${branchName}")
                            sh("curl --header PRIVATE-TOKEN:$TOKEN --request PUT    ${gitProjectUrl}/milestones/${milestoneId}?state_event=close")
                        }
                    }
                } catch(e) {
                    // continue if delete branch failed
                    print "Delete branch faild. please remove this release branch later."
                }
            }
            
            stage('Summary') {
                if (mattermostDevIncomingUrl && isIpcRunningEnv ) {
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

