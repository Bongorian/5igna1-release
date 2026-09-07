package com.bongorian.signa1;
import java.util.*;
public class RawGlitchCheck {
    public static void main(String[] args){
        int[] stages=Effects.ordered((1<<Effects.CFA_TEAR)|(1<<Effects.SENSOR_FAIL)|(1<<Effects.ROW_SHIFT),true);
        if(!Arrays.equals(stages,new int[]{Effects.SENSOR_FAIL,Effects.ROW_SHIFT,Effects.CFA_TEAR}))throw new AssertionError("Sensor pipeline order");
        byte[] ramp=new byte[128*96*2];for(int i=0;i<128*96;i++)RawGlitch.write(ramp,i,i%4096);
        byte[] expected=ramp;for(int stage:stages)expected=RawGlitch.apply(expected,128,96,4095,0,stage,2,17);
        if(!Arrays.equals(expected,RawGlitch.chain(ramp,128,96,4095,0,stages,2,17)))throw new AssertionError("RAW chain output");
        if(!Arrays.equals(ramp,RawGlitch.chain(ramp,128,96,4095,0,stages,0,17)))throw new AssertionError("Zero chain");
        if(Effects.ORDER[0]!=Effects.CLEAN)throw new AssertionError("Clean first");
        for(int[] dimensions:new int[][]{{128,96},{127,95},{1,1},{4096,3072}}){
            int w=dimensions[0],h=dimensions[1];byte[] original=new byte[w*h*2];for(int i=0;i<w*h;i++)RawGlitch.write(original,i,256+(i%3500));
            for(int mode:Effects.ordered(-1,true)){
                if(!Arrays.equals(original,RawGlitch.apply(original,w,h,4095,256,mode,0,17)))throw new AssertionError("Zero intensity changed RAW");
                for(float strength:new float[]{.05f,1f,4f}){
                    long start=System.nanoTime();byte[] actual=RawGlitch.apply(original,w,h,4095,256,mode,strength,17);int changed=0;
                    for(int i=0;i<w*h;i++){int value=RawGlitch.read(actual,i);if(value<0||value>4095)throw new AssertionError("Invalid sample");if(value!=RawGlitch.read(original,i))changed++;}
                    if(w>1000&&strength>=1&&changed==0)throw new AssertionError("No corruption for "+mode);
                    if(w<1000&&!Arrays.equals(actual,RawGlitch.apply(original,w,h,4095,256,mode,strength,17)))throw new AssertionError("Nondeterministic");
                    if(w==4096)System.out.printf(Locale.US,"PASS RAW %dx%d mode=%d power=%.2f changed=%.2f%% %.0fms%n",w,h,mode,strength,changed*100.0/(w*h),(System.nanoTime()-start)/1e6);
                }
            }
        }
    }
}
