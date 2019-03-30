package com.example.extractreader;

class PrnMask {
    int[] prnMask;
    int iodp;

    public PrnMask(){
        prnMask = new int[210];
        iodp = -1;
    }

    public PrnMask (int[] prnMask, int iodp){
        this.prnMask = prnMask;
        this.iodp = iodp;
    }
}
