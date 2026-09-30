package com.jonlee.android.common.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Need better idea to expand mathtest cases
 * E.g, using Question and Answer files that can be updated remotely
 */
public class MathTest {

    private final String TAG = "MathTest";
    private ArrayList number99List;
    private StringBuffer wholeQuestionAndAnswer;
    private int mathTestMultiplyScope;
    private int mathTestStartingNumber;
    private int mathTestEndingNumber;
    public static int mathTestModeAddition = 0;
    public static int mathTestModeMultiplication = 1;
    public static int mathTestModeSubtraction = 2;
    public static int mathTestModeDivision = 3;
    public static int mathTestModeCombination = 4;


//    public MathTest(int max){
//        mathTestMultiplyScope = max;
//        wholeQuestionAndAnswer = new StringBuffer();
//        number99List = new ArrayList();
//        createNumber99Map();
//    }

    public MathTest(int start, int end, int mode){
        //TO-DO: Do I need this?
//        mathTestMultiplyScope = Math.abs(end-start);
        wholeQuestionAndAnswer = new StringBuffer();
        number99List = new ArrayList();
        createNumber99Map(start, end, mode);
    }

//    public void createNumber99Map() {
//
//        for(int i = 1; i <= mathTestMultiplyScope; i++) {
//            for(int j=1; j <= mathTestMultiplyScope; j++) {
//                number99List.add( new QuestionDataSet(i, j));
//                wholeQuestionAndAnswer.append(i + " times " + j + " is \t " + i * j + "\n");
//            }
//        }
//    }

    public void createNumber99Map(int start, int end, int mode) {

        if( start > end ){
            //Swaping start and end numbers
            int temp = start;
            start = end;
            end = temp;
            Log.i(TAG, "Math.Starting number should be bigger than Ending number. Please check your settings.");

        }
        QuestionDataSet qds;
        for(int i=start; i <= end; i++) {
            for(int j=start; j <= end; j++) {
                number99List.add( qds = new QuestionDataSet(i, j, mode));
                wholeQuestionAndAnswer.append(qds.toString() + "\n");
            }
        }
    }

    /** TO-DO
    // Need to add Quiz logic here
     // Get input through Voice, and getting back to re
     */

    public ArrayList getNumber99Map(){
        return number99List;
    }

    public String getAll99DanString(){
        return wholeQuestionAndAnswer.toString();
    }

    public class QuestionDataSet{
        int x;
        int y;
        int result;
        int mode;
        String toString;

        QuestionDataSet(int x, int y){
            this.x = x;
            this.y = y;
            result = x*y;
            mode = MathTest.mathTestModeMultiplication;
        }

        QuestionDataSet(int x, int y, int mode){
            this.x = x;
            this.y = y;
            this.mode = mode;
            int tempIntForCombination = 0;
            if (mode == MathTest.mathTestModeAddition) {
                result = x + y;
                toString = x + " plus " + y + " is " + result;
            }
            else if (mode == MathTest.mathTestModeDivision) {
                // x/y = z
                // 나누기 문제 쉽게 만들기.
                x = x*y;

                result = x / y;
                toString = x + " over " + y + " is " + result;
            }
            else if (mode == MathTest.mathTestModeMultiplication) {
                result = x * y;
                toString = x + " times " + y + " is " + result;
            }
            else if (mode == MathTest.mathTestModeSubtraction) {
                // No negative quiz yet.
                // Swapping
                if(x < y){
                    int z = y;
                    x = y;
                    y = z;
                }
                result = x - y;
                toString = x + " minus " + y + " is " + result;
            }
            // To-do: avoid duplicated function.
            // recursive module
            else if (mode == MathTest.mathTestModeCombination){
                tempIntForCombination = new Random().nextInt((3 - 0) + 1) + 0;
                //Only for sub, add, multi
                if (tempIntForCombination == MathTest.mathTestModeAddition) {
                    result = x + y;
                    toString = x + " plus " + y + " is " + result;
                }
                else if (tempIntForCombination == MathTest.mathTestModeDivision) {
                    // x/y = z
                    // 나누기 문제 쉽게 만들기. Answer is the original x value
                    x = x*y;

                    result = x / y;
                    toString = x + " over " + y + " is " + result;

                    result = x / y;
                    toString = x + " over " + y + " is " + result;
                }
                else if (tempIntForCombination == MathTest.mathTestModeMultiplication) {
                    result = x * y;
                    toString = x + " times " + y + " is " + result;
                }
                else if (tempIntForCombination == MathTest.mathTestModeSubtraction) {
                    // No negative quiz yet.
                    // Swapping
                    if(x < y){
                        int z = y;
                        x = y;
                        y = z;
                    }
                    result = x - y;
                    toString = x + " minus " + y + " is " + result;
                }
            }
            else
                result = x*y; //Multiplication is default
//                toString = x + " times " + y + " is " + result;
            }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getMatchTestMode() { return mode; }

        public String toString(){
            Log.i(TAG, "MathTest.toString() ---> " + toString);
            return toString;
        }
    }

    private void setDataSetAndString(int x, int y, int mode){

    }

    public String getRamdomTest(int n){
        StringBuffer result = new StringBuffer();

        int max = this.number99List.size();
        int luckyNumber = (new Random()).nextInt(max);

        for(int i = 0; i < n; i++){
            if(luckyNumber == max-1){
                luckyNumber = -1;
            }
            result.append((this.number99List.get(++luckyNumber)).toString() + "\n");
            Log.i(TAG, "i:n:luckynumber ---> " + i + ":" +n + ":" + luckyNumber + " = "
                    + (this.number99List.get(luckyNumber)).toString() + "\n");
        }

        return result.toString();
    }


}
