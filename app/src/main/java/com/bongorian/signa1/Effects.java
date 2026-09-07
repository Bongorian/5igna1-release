package com.bongorian.signa1;
final class Effects {
    static final String[] NAMES={"CLEAN","SENSOR FAIL","EXPOSURE BAND","ROW SHIFT","LINE LOSS","BIT ROT","DATA SHIFT","CFA TEAR","CFA OFFSET","DEMOSAIC","CHROMA","SPECTRUM","CHROMA LOSS","CORRUPT","PACKET LOSS","VHS","TERMINAL"};

    // v6 IDs follow the capture pipeline. v5 persisted IDs are intentionally reset.
    static final int CLEAN=0;
    static final int SENSOR_FAIL=1;
    static final int EXPOSURE_BAND=2;
    static final int ROW_SHIFT=3;
    static final int LINE_LOSS=4;
    static final int BIT_ROT=5;
    static final int DATA_SHIFT=6;
    static final int CFA_TEAR=7;
    static final int CFA_OFFSET=8;
    static final int DEMOSAIC=9;
    static final int CHROMA=10;
    static final int SPECTRUM=11;
    static final int CHROMA_LOSS=12;
    static final int CORRUPT=13;
    static final int PACKET_LOSS=14;
    static final int VHS=15;
    static final int TERMINAL=16;
    static final int[] ORDER=java.util.stream.IntStream.range(0,NAMES.length).toArray();
    static boolean display(int id){return id==VHS||id==TERMINAL;}
    static boolean raw(int id){return id>=SENSOR_FAIL&&id<=CFA_OFFSET;}
    static String shaderDefines(){StringBuilder out=new StringBuilder();for(int id:ORDER)out.append("#define FX_").append(NAMES[id].replace(' ','_')).append(' ').append(id).append('\n');return out.toString();}
    static boolean available(int id,boolean video,boolean rawOnly){return id>=CLEAN&&id<NAMES.length&&(id!=PACKET_LOSS||video)&&(!rawOnly||id==CLEAN||raw(id));}
    static int[] choices(boolean video,boolean rawOnly){return java.util.Arrays.stream(ORDER).filter(id->available(id,video,rawOnly)).toArray();}
    static int[] active(int mode,int mask,boolean video,boolean rawOnly){return mask==0?(available(mode,video,rawOnly)?new int[]{mode}:new int[0]):java.util.Arrays.stream(ORDER).filter(id->id!=CLEAN&&(mask&(1<<id))!=0&&available(id,video,rawOnly)).toArray();}
    static String stage(int id){
        if(id==SENSOR_FAIL||id==EXPOSURE_BAND)return "01 / SENSOR";
        if(id==ROW_SHIFT||id==LINE_LOSS)return "02 / READOUT";
        if(id==BIT_ROT||id==DATA_SHIFT)return "03 / DATA";
        if(id==CFA_TEAR||id==CFA_OFFSET)return "04 / CFA";
        if(id==DEMOSAIC)return "05 / ISP";
        if(id==CHROMA||id==SPECTRUM||id==CHROMA_LOSS)return "06 / COLOR";
        if(id==CORRUPT||id==PACKET_LOSS)return "07 / TRANSPORT";
        return "08 / DISPLAY";
    }
    static int next(int id,int delta){for(int i=0;i<ORDER.length;i++)if(ORDER[i]==id)return ORDER[Math.floorMod(i+delta,ORDER.length)];return CLEAN;}
    static int[] ordered(int mask,boolean rawOnly){return java.util.Arrays.stream(ORDER).filter(id->id!=CLEAN&&(mask&(1<<id))!=0&&(!rawOnly||raw(id))).toArray();}
    static String chainName(int[] chain){if(chain.length==0)return name(CLEAN);StringBuilder s=new StringBuilder();for(int id:chain){if(s.length()>0)s.append(" → ");s.append(name(id));}return s.toString();}
    static String name(int n){return NAMES[Math.max(0,Math.min(NAMES.length-1,n))];}
}
