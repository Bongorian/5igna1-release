package com.bongorian.signa1;

/** Corrupts RAW16 samples; keeps the container and sample bounds valid. */
final class RawGlitch {
    static int mix(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;x*=0x846ca68b;x^=x>>>16;return x;}
    static float random(int x){return (mix(x)&0x7fffffff)/2147483648f;}
    static int read(byte[] data,int index){int p=index*2;return (data[p]&255)|((data[p+1]&255)<<8);}
    static void write(byte[] data,int index,int value){int p=index*2;data[p]=(byte)value;data[p+1]=(byte)(value>>>8);}
    static byte[] chain(byte[] input,int w,int h,int white,int black,int[] modes,float power,int seed){return chain(input,w,h,white,black,modes,power,seed,EffectParameters.defaults());}
    static byte[] chain(byte[] input,int w,int h,int white,int black,int[] modes,float power,int seed,float[] parameters){
        return chain(input,w,h,white,black,modes,power,seed,parameters,null);
    }
    static byte[] chain(byte[] input,int w,int h,int white,int black,int[] modes,float power,int seed,float[] parameters,float[] live){
        byte[] result=input;for(int mode:modes)if(Effects.raw(mode))result=apply(result,w,h,white,black,mode,power,seed,parameters,live);return result;
    }
    static byte[] apply(byte[] input,int w,int h,int white,int black,int mode,float power,int seed){return apply(input,w,h,white,black,mode,power,seed,EffectParameters.defaults());}
    static byte[] apply(byte[] input,int w,int h,int white,int black,int mode,float power,int captureSeed,float[] parameters){
        return apply(input,w,h,white,black,mode,power,captureSeed,parameters,null);
    }
    static byte[] apply(byte[] input,int w,int h,int white,int black,int mode,float power,int captureSeed,float[] parameters,float[] live){
        if(w<=0||h<=0||input.length!=(long)w*h*2)throw new IllegalArgumentException("RAW dimensions");
        byte[] output=input.clone();power=EffectParameters.unit(power)*EffectParameters.get(parameters,mode,0);if(power==0||!Effects.raw(mode))return output;
        float shape=EffectParameters.get(parameters,mode,1),character=EffectParameters.get(parameters,mode,2);
        int seed=(int)(EffectParameters.get(parameters,mode,3)*1000003),bits=32-Integer.numberOfLeadingZeros(Math.max(1,white));
        int rows=(int)(240-shape*228),tile=Math.max(8,(int)(8+shape*248)),bitBlock=Math.max(2,(int)(2+character*126));
        for(int y=0;y<h;y++){
            int rowSeed=seed+(int)((long)(y&~1)*rows/h)*193;boolean band=random(rowSeed)<power;
            int shift=band?Math.round((random(rowSeed+21)-.5f)*w*character*.6f*power/2)*2:0;
            float exposure=1-power*(.5f+.5f*(float)Math.sin(y*(12+shape*168.0)/h+captureSeed*.001*character+seed));
            int at=mode*4;
            if(live!=null&&live[at]>0){
                if(mode==Effects.ROW_SHIFT)shift+=Math.round((y/(float)h-.5f)*live[at+1]*w*power/2)*2;
                if(mode==Effects.EXPOSURE_BAND)exposure=1-power*(.5f+.5f*live[at+3]*(float)Math.sin(y/(float)h*live[at+2]+live[at+1]));
            }
            for(int x=0;x<w;x++){
                int sx=x,sy=y;
                if(mode==Effects.ROW_SHIFT)sx=x+shift;
                if(mode==Effects.CFA_TEAR){int block=seed+(x/tile)*71+(y/Math.max(1,tile/2))*137;if(random(block)<power*.88f){if(character<.25f||character>=.75f)sx=(x+1)%w;if(character>=.25f)sy=(y+1)%h;}}
                // Repeat whole Bayer row pairs so LINE LOSS never becomes CFA OFFSET.
                if(mode==Effects.LINE_LOSS){
                    int bandHeight=2*(1+Math.round(shape*63)),start=(y/bandHeight)*bandHeight;
                    if(random(seed+(y/bandHeight)*193)<power){
                        int previous=start-2+(y&1);
                        int repeated=previous<0?black:read(input,previous*w+x);
                        write(output,y*w+x,Math.max(0,Math.min(white,black+Math.round((repeated-black)*character))));continue;
                    }
                }
                if(mode==Effects.CFA_OFFSET){
                    int size=2*(1+Math.round(character*127));
                    if(random(seed+(x/size)*71+(y/size)*137)<power){
                        int phase=Math.round(shape*2);
                        sx=phase==1?x:x^1;sy=phase==0?y:y^1;
                        // Odd-sized borders have no partner; retain the original sample.
                        if(sx>=w||sy>=h){sx=x;sy=y;}
                    }
                }
                if(mode==Effects.DATA_SHIFT){
                    int bytes=2*(2+Math.round(character*254)),offset=Math.round(shape*31),index=(y*w+x)*2;
                    if(offset>0&&random(seed+(index/bytes)*733)<power){
                        int src=index+offset;
                        int value=src+1<input.length?(input[src]&255)|((input[src+1]&255)<<8):black;
                        write(output,y*w+x,Math.max(0,Math.min(white,value)));continue;
                    }
                }
                int value=sx<0||sx>=w?black:read(input,sy*w+sx);
                if(mode==Effects.BIT_ROT){int block=seed+(x/bitBlock)*733+(y/Math.max(1,bitBlock/2))*97;if(random(block)<power*.8f)value^=1<<Math.round(shape*(bits-1));}
                if(mode==Effects.SENSOR_FAIL){float col=random(seed+x*4099),point=random(seed+x*37+y*701);if(col<power*.06f*shape)value=random(seed+x*4099+71)<character?white:black;else if(point<power*.006f*(1-shape))value=random(seed+x*37+y*701+71)<character?white:black;}
                if(mode==Effects.EXPOSURE_BAND)value=black+Math.round((value-black)*exposure);
                write(output,y*w+x,Math.max(0,Math.min(white,value)));
            }
        }
        return output;
    }
}
