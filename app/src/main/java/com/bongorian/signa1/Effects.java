package com.bongorian.signa1;

import java.util.Arrays;

/** Owns fault IDs and causal order. IDs are deliberately incompatible with release 1.0. */
final class Effects {
    enum Point { SENSOR, READOUT, DATA, RECONSTRUCTION, COLOR, STREAM, MEDIA, DISPLAY }
    static final int CLEAN=0,PIXEL_DAMAGE=1,EXPOSURE=2,ROW_ERROR=3,BIT_ERROR=4,ADDRESS_ERROR=5,
        CFA_ERROR=6,DEMOSAIC_ERROR=7,CHROMA_ERROR=8,COLOR_MAP=9,BLOCK_ERROR=10,STREAM_ERROR=11,VHS=12,CRT=13;
    static final String[] NAMES={"CLEAN","PIXEL DAMAGE","EXPOSURE","ROW ERROR","BIT ERROR","ADDRESS ERROR",
        "CFA ERROR","DEMOSAIC ERROR","CHROMA ERROR","COLOR MAP","BLOCK ERROR","STREAM ERROR","VHS","CRT"};
    static final int[] ORDER=java.util.stream.IntStream.range(0,NAMES.length).toArray();
    static final class Control {
        final String key; final float initial;
        Control(String key,float initial){this.key=key;this.initial=initial;}
    }
    private static Control c(String key,float initial){return new Control(key,initial);}
    // Compact artistic controls, not a fixed renderer ABI. Each fault can add its own controls.
    static final Control[][] CONTROLS={ {},
        {c("density",.5f),c("hot",.5f),c("columns",.2f)},
        {c("depth",.6f),c("rate",.35f),c("bands",.4f)},
        {c("displacement",.5f),c("bands",.4f),c("loss",.35f),c("concealment",.7f)},
        {c("activity",.5f),c("bit",.6f),c("burst_size",.4f)},
        {c("offset",.35f),c("region",.4f),c("activity",.6f)},
        {c("coverage",.55f),c("phase",0f),c("region",.4f)},
        {c("interpolation",.65f),c("sampling",.4f)},
        {c("separation",.45f),c("sampling",.35f),c("direction",0f)},
        {c("palette",.5f),c("cycles",.4f),c("mix",.8f)},
        {c("quantization",.5f),c("block_size",.45f),c("misaddress",.4f)},
        {c("loss",.5f),c("region",.4f),c("concealment",.7f)},
        {c("bandwidth",.6f),c("tracking",.5f),c("dropout",.4f),c("noise",.25f)},
        {c("scan",.5f),c("phosphor",0f),c("convergence",.4f),c("sync",.4f)}
    };
    static Point point(int id){
        if(id<=EXPOSURE)return Point.SENSOR;if(id==ROW_ERROR)return Point.READOUT;
        if(id<=ADDRESS_ERROR)return Point.DATA;if(id<=DEMOSAIC_ERROR)return Point.RECONSTRUCTION;
        if(id<=COLOR_MAP)return Point.COLOR;if(id<=STREAM_ERROR)return Point.STREAM;
        return id==VHS?Point.MEDIA:Point.DISPLAY;
    }
    static String stage(int id){String[] names={"01 / SENSOR","02 / READOUT","03 / DATA","04 / CFA / RECONSTRUCTION","05 / COLOR","06 / CODEC / STREAM","07 / MEDIA","08 / DISPLAY"};return names[point(id).ordinal()];}
    static boolean raw(int id){return id>=PIXEL_DAMAGE&&id<=CFA_ERROR;}
    static boolean available(int id,boolean video,boolean rawOnly){return id>=0&&id<NAMES.length&&(!rawOnly||id==CLEAN||raw(id));}
    static int[] choices(boolean video,boolean rawOnly){return Arrays.stream(ORDER).filter(id->available(id,video,rawOnly)).toArray();}
    static int[] ordered(int mask,boolean rawOnly){return Arrays.stream(ORDER).filter(id->id!=CLEAN&&(mask&(1<<id))!=0&&(!rawOnly||raw(id))).toArray();}
    static String shaderDefines(){StringBuilder s=new StringBuilder();for(int id:ORDER)s.append("#define FX_").append(NAMES[id].replace(' ','_')).append(' ').append(id).append('\n');return s.toString();}
    static String chainName(int[] ids){if(ids.length==0)return NAMES[CLEAN];StringBuilder s=new StringBuilder();for(int id:ids){if(s.length()>0)s.append(" → ");s.append(name(id));}return s.toString();}
    static String name(int id){return NAMES[Math.max(0,Math.min(NAMES.length-1,id))];}
}
