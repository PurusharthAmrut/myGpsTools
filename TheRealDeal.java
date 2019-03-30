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

    PrnMask oldPrnMask;
    PrnMask currentPrnMask;

    private String message;
    private int iodp, firstIodp;
    int positionVariable;
    int[] prnMask = new int[210];
    int weekNum;
    float weekSecs;
    SpaceVehicle[] svList = SpaceVehicle.setSpaceVehicleArray(210);

    public static final int IODP_NOT_FOUND_ERROR = -1;
    public static final int IODP_MISMATCH_ERROR = -2;
    public static final int PRN_MASK_POPULATION_ERROR = -3;
    public static final int MESSAGE_INTERPRETATION_ERROR = -4;
    public static final int MESSAGE_2TO5_NOT_FOUND_ERROR = -12;
    int CHECK_OLD_PRN_MASK = -1;    /*This is a flag, which is initially set to -1, meaning the oldPrnMask object need not be checked
                                     * in functions like interpretMessageType2to5*/


    public int messageExtract(String fileName){
        message = getMessageFromFile(fileName);

        firstIodp = getFirstIodp();
        populatePrnMaskAndFixLastIodp();
        currentPrnMask = new PrnMask(prnMask, iodp);
        
        if (iodp != firstIodp) {    //oldPrnMask will be initialised only is the first and last iodp are not equal
            interpretMessageType1(message.indexOf("WAAS1A"));   //NOTE: Here, I have assumed that the PRN mask has only changed once in the whole message!
            oldPrnMask = new PrnMask(prnMask, iodp);
            CHECK_OLD_PRN_MASK = 1; //Flag is raised
            //return IODP_MISMATCH_ERROR;     //Here, this may also mean that interpretMessageType1() has failed
        }

        if ((positionVariable = message.lastIndexOf("WAAS2A"))>0){
            interpretMessageType2to5(2, positionVariable);
        }

        if ((positionVariable = message.lastIndexOf("WAAS3A"))>0){
            interpretMessageType2to5(3, positionVariable);
        }

        if ((positionVariable = message.lastIndexOf("WAAS4A"))>0){
            interpretMessageType2to5(4, positionVariable);
        }

        if ((positionVariable = message.lastIndexOf("WAAS5A"))>0){
            interpretMessageType2to5(5, positionVariable);
        }

        else
            return MESSAGE_2TO5_NOT_FOUND_ERROR;

        /*Pattern messageType = Pattern.compile("\\bWAAS[2345]A\\b");
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
                if(distributeMessageInterpretation(m.group(), messagePosition)!=1)   //m.group() will contain just the message heading.
                    return MESSAGE_INTERPRETATION_ERROR;
            }
        }while(messagePosition++>=0);*/
        return 1;
    }

 /*   private int distributeMessageInterpretation(String messageHeading, int messagePosition){

        switch(messageHeading){
            case "WAAS1A":
                if (interpretMessageType1(messagePosition)<0)
                    return MESSAGE_INTERPRETATION_ERROR;
                break;
            case "WAAS2A":
            case "WAAS3A":
            case "WAAS4A":
            case "WAAS5A":
                interpretMessageType2to5(messageHeading, messagePosition);
                break;
            default:
                return MESSAGE_INTERPRETATION_ERROR;
        }
        return 1;
    }*/

    private int interpretMessageType1(int messagePosition){      //Consider error handling based on return value

        Pattern iodpPattern = Pattern.compile("IODP:([0-3])");      //Exception handling in case IODP is not in the range 0-3 (which is most probably not possible)
        Pattern prnBitPattern = Pattern.compile("[01]");            //Same as above
        Matcher matchPrnBitPattern = prnBitPattern.matcher(message);
        Matcher matchIodpPattern = iodpPattern.matcher(message);

        matchIodpPattern = matchIodpPattern.region(messagePosition, matchIodpPattern.regionEnd());
        if (matchIodpPattern.find()){
            iodp = Integer.parseInt(matchIodpPattern.group(1));
        }
        else{
            return IODP_NOT_FOUND_ERROR;
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
                return PRN_MASK_POPULATION_ERROR;
            }
        }
        return 1;
    }

    private void interpretMessageType2to5(int messageType, int messagePosition){

        Pattern iodp2to5 = Pattern.compile("IODP:([0-3])");
        Pattern fcPattern = Pattern.compile("(-*[0-9]+\\.[0-9]+)\\b");
        Pattern udreiPattern = Pattern.compile("([0-9]+)\\b");      //Here, there's no verification of the udrei number
        Pattern satTimePattern = Pattern.compile("SATTIME:([0-9]+),([0-9]+\\.[0-9]+)\\b");

        Matcher matchIodp2to5 = iodp2to5.matcher(message);
        Matcher matchfcPattern = fcPattern.matcher(message);
        Matcher matchUdreiPattern = udreiPattern.matcher(message);
        Matcher matchSatTimePattern = satTimePattern.matcher(message);

        int position = message.indexOf("SATTIME:", messagePosition);

        matchSatTimePattern = matchSatTimePattern.region(position, (position = message.indexOf("IODP:", position)));
        matchIodp2to5 = matchIodp2to5.region(position, (position = message.indexOf("Fast", position))); //the second argument does 2 works at one using side effects of expressions
        matchfcPattern = matchfcPattern.region(position, (position = message.indexOf("UDREI", position)));
        matchUdreiPattern = matchUdreiPattern.region(position, message.indexOf("\r\n", position + 7));

        matchSatTimePattern.find();
        weekNum = Integer.parseInt(matchSatTimePattern.group(1));
        weekSecs = Float.parseFloat(matchSatTimePattern.group(2));

        matchIodp2to5.find();
        iodp = Integer.parseInt(matchIodp2to5.group(1));
        if (iodp == currentPrnMask.iodp)
            prnMask = currentPrnMask.prnMask;
        else if (CHECK_OLD_PRN_MASK==1 && iodp == oldPrnMask.iodp)
            prnMask = oldPrnMask.prnMask;
        else
            return;     //If the IODP of message matches neither, then it is ignored

        int activeSatCount = 0, startCount, endCount;
        switch (messageType){
            case 2:
                startCount = 1;
                endCount = 13;
                break;
            case 3:
                startCount = 14;
                endCount = 26;
                break;
            case 4:
                startCount = 27;
                endCount = 39;
                break;
            case 5:
                startCount = 40;
                endCount = 51;      //Not 52 because not more than 51 satellites' corrections can be broadcast in order to maintain integrity
                break;
            default:
                return;
        }
        /*The following loop takes care of the fact that as soon as the new message comes, the array region is reset, as the previous message becomes invalid
        * Loop to be made!*/

        for (int i = 0; i<prnMask.length; i++){
            if (prnMask[i] == 1){
                activeSatCount++;
                if (activeSatCount > endCount)
                    break;
                if (activeSatCount >= startCount){
                    svList[i].weekNum = weekNum;
                    svList[i].weekSecs = weekSecs;
                    if (matchfcPattern.find() && matchUdreiPattern.find()){
                        svList[i].fastCorrection = Float.parseFloat(matchfcPattern.group(1));
                        svList[i].udrei = Integer.parseInt(matchUdreiPattern.group(1));
                    }
                }
            }
        }
    }

    private String getMessageFromFile(String fileName){
        String text;
        try{
            File textfile = new File(Environment.getExternalStorageDirectory(), fileName);
            FileInputStream fis = new FileInputStream(textfile);

            int size = fis.available();
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

    //The first iodp is checked with the last iodp to ensure that the PRN mask has been retained throughout the message
    /*Loophole: If the PRN mask may change 4 times, the iodp will circle back to the same value by the end of the message.
    * This is assumed to be very rare*/
    private int getFirstIodp(){
        int firstPrnPosition = message.indexOf("WAAS1A");
        Pattern iodp = Pattern.compile("IODP:([0-3])");
        Matcher matcher = iodp.matcher(message);
        matcher = matcher.region(firstPrnPosition, firstPrnPosition + 15);    //15 is a safe limit in which we are bound to find the IODP

        if (matcher.find()){
            return Integer.parseInt(matcher.group(1));
        }
        else
            return IODP_NOT_FOUND_ERROR;      //In case this function has failed to get the iodp
    }

    /*This function not only extracts the most recent (last) PRN bit mask, but also returns back the
    * IODP of the last WAAS1A message, which helps to cross check whether the PRN mask has changed
    * through the course of the message*/
    private int populatePrnMaskAndFixLastIodp(){
        int lastPrnPosition = message.lastIndexOf("WAAS1A");
        return interpretMessageType1(lastPrnPosition);
    }
}
