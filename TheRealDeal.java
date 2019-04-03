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

    PrnMask[] prnMasks;
    SpaceVehicle[] svList = SpaceVehicle.setSpaceVehicleArray(210);

    private String message;

    //The following fields are used as temporary storage spaces by different functions
    private int iodp;
    int positionVariable;
    int[] prnMask = new int[210];

    int weekNum;
    float weekSecs;

    private static final Pattern iodpPattern = Pattern.compile("IODP:([0-3])");

    public static final int IODP_NOT_FOUND_ERROR = -1;
    public static final int IODP_MISMATCH_ERROR = -2;
    public static final int PRN_MASK_POPULATION_ERROR = -3;
    public static final int MESSAGE_INTERPRETATION_ERROR = -4;
    public static final int MESSAGE_2TO5_NOT_FOUND_ERROR = -12;
    int CHECK_OLD_PRN_MASK = -1;    /*This is a flag, which is initially set to -1, meaning the oldPrnMask object need not be checked
                                     * in functions like interpretMessageType2to5*/


    public int messageExtract(String fileName){
        message = getMessageFromFile(fileName);
        archiveAllIodps();

        /*firstIodp = getFirstIodp();
        populatePrnMaskAndFixLastIodp();
        currentPrnMask = new PrnMask(prnMask, iodp);
        
        if (iodp != firstIodp) {    //oldPrnMask will be initialised only is the first and last iodp are not equal
            interpretMessageType1(message.indexOf("WAAS1A"));   //NOTE: Here, I have assumed that the PRN mask has only changed once in the whole message!
            oldPrnMask = new PrnMask(prnMask, iodp);
            CHECK_OLD_PRN_MASK = 1; //Flag is raised
            //return IODP_MISMATCH_ERROR;     //Here, this may also mean that interpretMessageType1() has failed
        }*/

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

        Pattern fcPattern = Pattern.compile("(-*[0-9]+\\.[0-9]+)\\b");
        Pattern udreiPattern = Pattern.compile("([0-9]+)\\b");      //Here, there's no verification of the udrei number
        Pattern satTimePattern = Pattern.compile("SATTIME:([0-9]+),([0-9]+\\.[0-9]+)\\b");

        Matcher matchIodp2to5 = iodpPattern.matcher(message);
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
        int j = 0;
        boolean IODP_FOUND = false;
        while (prnMasks[j] != null){
            if (prnMasks[j].iodp == iodp){
                prnMask = prnMasks[j].prnMask;
                IODP_FOUND = true;
                break;
            }
            j++;
        }
        if (!IODP_FOUND)
            return;

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
            else if (prnMask[i] == 0){          //To clear out the previous corrections
                svList[i].fastCorrection = 0;
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

    /*The following function surfs through the entire correction file, looking for WAAS1A messages, and registers
    * all PRN bit masks with different IODPs into an array of PrnMask objects  called prnMasks[]*/
    public void archiveAllIodps(){

        prnMasks = new PrnMask[4];      //Ready for the rare occasion when there are 3 changes in PRN mask with IODPs 0,1,2,3
        positionVariable = -1;      //So that positionVariable + 1 will be 0 in the first iteration
        Matcher matchIodpPattern = iodpPattern.matcher(message);

        while ((positionVariable = message.indexOf("WAAS1A", positionVariable+1)) >= 0){
            matchIodpPattern = matchIodpPattern.region(positionVariable, positionVariable + 15);
            matchIodpPattern.find();
            iodp = Integer.parseInt(matchIodpPattern.group(1));
            for (int i = 0; i < 4; i++){
                /*/The following condition means that if the referenced object is null, it means that it is either the first object, or all the
                * previous objects' iodps did not match the iodp in hand*/
                if (prnMasks[i] == null){
                    prnMasks[i] = new PrnMask();
                    interpretMessageType1(positionVariable);
                    prnMasks[i].iodp = iodp;
                    prnMasks[i].prnMask = prnMask;
                }
                else if (prnMasks[i].iodp == iodp)
                    break;
            }
        }
    }
}
