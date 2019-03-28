package com.example.extractreader;

import android.app.Activity;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.example.extractreader.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TheRealDeal {

    private String message;
    private int iodp;
    int positionVariable;
    int[] prnMask = new int[210];



    void messageExtract(String fileName){
        message = getMessageFromFile(fileName);
        int messagePosition=0;

        Pattern messageType = Pattern.compile("\\bWAAS[12345]A\\b");
        Matcher m = messageType.matcher(message);

        do{
            messagePosition = message.indexOf("WAAS", messagePosition);     //messagePosition holds position of the next message type heading
            try{
                m = m.region(messagePosition, messagePosition+6);                   //Confining the region so that m does not have to search the whole message
            }catch(IndexOutOfBoundsException e){
                e.printStackTrace();
                //Toast.makeText(mainActivity.getApplicationContext(), "Message reading completed", Toast.LENGTH_SHORT).show();
            }

            if (m.find()){
                if(distributeMessageInterpretation(m.group(), messagePosition)==0)   //m.group() will contain just the message heading.
                    break;
            }
        }while(messagePosition++>=0);
    }

    public int distributeMessageInterpretation(String messageHeading, int messagePosition){

        switch(messageHeading){
            case "WAAS1A":
                interpretMessageType1(messagePosition);
                return 0;
            case "WAAS2A":
            case "WAAS3A":
            case "WAAS4A":
            case "WAAS5A":
                interpretMessageType2to5(messageHeading, messagePosition);
                break;
            default:
                return -1;
        }
        return 1;
    }

    public int interpretMessageType1(int messagePosition){      //Consider error handling based on return value

        Pattern iodpPattern = Pattern.compile("IODP:([0-3])");      //Exception handling in case IODP is not in the range 0-3 (which is most probably not possible)
        Pattern prnBitPattern = Pattern.compile("[01]");            //Same as above
        Matcher matchPrnBitPattern = prnBitPattern.matcher(message);
        Matcher matchIodpPattern = iodpPattern.matcher(message);

        matchIodpPattern = matchIodpPattern.region(messagePosition, matchIodpPattern.regionEnd());
        if (matchIodpPattern.find()){
            iodp = Integer.parseInt(matchIodpPattern.group(1));
        }
        else{
            return -1;
        }

        positionVariable = message.indexOf(":", messagePosition);
        //Travelling from the colon to the start of the PRN bit mask. IODP:3\r\n<PRN bit mask>
        //Now messagePosition is the position of the first bit of PRN bit mask
        positionVariable += 4;
        //Matching region is confined beyond the IODP because IODP (ranging from 0 to 3) might also be matched
        matchPrnBitPattern = matchPrnBitPattern.region(positionVariable, positionVariable + 216);   //PRN bit mask length is 216, of which the 6 bits are part of extra padding

        for(int i=0; i<210;i++){
            if (matchPrnBitPattern.find()){
                prnMask[i] = Integer.parseInt(matchPrnBitPattern.group());
            }
            else{                                       //Throwing an exception would be advisable
                return -1;
            }
        }
        return 1;
    }

    public void interpretMessageType2to5(String messageType, int messagePosition){

    }

    public String getMessageFromFile(String fileName){
        String text;
        try{
            File textfile = new File(Environment.getExternalStorageDirectory(), fileName);
            FileInputStream fis = new FileInputStream(textfile);

            int size = 1000/*fis.available()*/;
            byte[] buffer = new byte[size];

            fis.read(buffer);
            fis.close();

            text = new String(buffer);
        }catch(Exception e){
            e.printStackTrace();
            return MainActivity.FILE_READ_ERROR;
        }
        return text;
    }
}
