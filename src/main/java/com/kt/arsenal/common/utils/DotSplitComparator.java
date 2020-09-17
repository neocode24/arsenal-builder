package com.kt.arsenal.common.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Arsenal-Dev
 *
 * 구분자 "."로 되어 있는 String 문자열을 Split 하여, 각 Index 마다 숫자형식으로 비교 함.
 * Index 항목이 숫자가 아닌경우 String 비교로 전환함.
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/08/25
 */

@FunctionalInterface
public interface DotSplitComparator<T> extends Comparator {

    static <T, U> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor) {

        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
                (c1, c2) -> {
                    int r = 0;
                    try {
                        int[] arrA = Arrays.stream(String.valueOf(keyExtractor.apply(c1)).split("\\.")).mapToInt(Integer::parseInt).toArray();
                        int[] arrB = Arrays.stream(String.valueOf(keyExtractor.apply(c2)).split("\\.")).mapToInt(Integer::parseInt).toArray();

                        for (int i = 0; i < arrA.length && i < arrB.length; i++) {
                            r = Integer.compare(arrA[i], arrB[i]);
                            if (r != 0) {
                                return r;
                            }
                        }
                    } catch (NumberFormatException e) {
                        r = String.valueOf(keyExtractor.apply(c1)).compareTo(String.valueOf(keyExtractor.apply(c2)));
                    }
                    return r;
                };
    }


}
