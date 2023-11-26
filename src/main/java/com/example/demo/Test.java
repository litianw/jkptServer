package com.example.demo;

public class Test {
    public static void main(String[] args) {

        String s = "abaacfvcc";
        char[] array = s.toCharArray();
        String r = "";
        for (int i = 0; i <array.length ; i++) {
            r+=array[i];
            int count =  f(array,i);

            i+=count-1;
            r+="_"+count+"_";

        }
        System.out.println( r.substring(0,r.length()-1));
    }

    static int f(char[] array, int index){
        if(index==array.length-1){
            return 1;
        }
        int count = 1;
        if(array[index] == array[index+1]){
          return  count+f(array, index+1);
        }
       return count;
    }
}
