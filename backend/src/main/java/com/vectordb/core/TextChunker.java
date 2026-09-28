package com.vectordb.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextChunker {
    // Spliting the text into Chunks of size 250 with overlap of 30 
    public static final int DEFAULT_CHUNK_WORDS=250;
    public static final int DEFAULT_OVERLAP_WORDS=30;

    private TextChunker(){
        // utility class
    }
    public static List<String> chunk(String text){
        return chunk(text,DEFAULT_CHUNK_WORDS,DEFAULT_OVERLAP_WORDS);

    }
    //Core Method

    public static List<String> chunk(String text, int chunkWords,int overlapWords){
        validateParameters(chunkWords,overlapWords);

        List<String> chunks =new ArrayList<>();
// prevents empty files upload
        if(text==null || text.isBlank()){
            return chunks;
        }
        //tokenization
        String[] words =tokenize(text);

        if(words.length==0){
            return chunks;
        }
        
        if (words.length<= chunkWords){
            chunks.add(join(words,0,words.length));
            return chunks;
        }
        int step =chunkWords-overlapWords;
        int start =0;
        
        while(start<words.length){
            int end =Math.min(start + chunkWords,words.length);
            chunks.add(join(words,start,end));

            if(end==words.length){
                break;
            }
            start+= step;
        }
        return chunks;
    }
    public static int wordCount(String text){
        if(text==null || text.isBlank()){
            return 0;
        }
        return tokenize(text).length;
    }
    private static String[] tokenize(String text){
        return text.strip().split("\\s+");
    }
    private static String join(String[] words,int from,int to){
        return String.join(" ",Arrays.copyOfRange(words,from, to));
    }

    private static void validateParameters(int chunkWords,int overlapWords){
        if(chunkWords<=0){
            throw new IllegalArgumentException(
                "chunkWords must be >0,got:" + chunkWords);
        }
       if (overlapWords < 0) {
            throw new IllegalArgumentException(
                    "overlapWords must be >= 0, got: " + overlapWords);
        }
        if (overlapWords >= chunkWords) {
            throw new IllegalArgumentException(
                    "overlapWords (" + overlapWords + ") must be < chunkWords ("
                    + chunkWords + ")");
        }
    }
}

