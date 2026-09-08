package com.bongorian.signa1;

/** RAW16 representation adapter for the SAME immutable fault nodes as RGB/video. */
final class RawGlitch {
    static int read(byte[] data,int index){int p=index*2;return (data[p]&255)|((data[p+1]&255)<<8);}
    static void write(byte[] data,int index,int value){int p=index*2;data[p]=(byte)value;data[p+1]=(byte)(value>>>8);}
    static byte[] chain(byte[] input,int w,int h,int white,int black,EffectState.Frame frame){
        if(w<=0||h<=0||input.length!=(long)w*h*2||white<1||white>65535||black<0||black>white)throw new IllegalArgumentException("RAW dimensions/levels");
        byte[] output=input.clone();for(FaultNode node:frame.nodes)if(Effects.raw(node.id))output=apply(output,w,h,white,black,node);return output;
    }
    private static float hash(long seed,int x,int y){return FaultModel.random(seed^FaultModel.mix(((long)x<<32)^(y&0xffffffffL)));}
    static byte[] apply(byte[] input,int w,int h,int white,int black,FaultNode n){
        byte[] out=input.clone();long seed=n.identity.seed;int bits=32-Integer.numberOfLeadingZeros(white);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int sx=x,sy=y,index=y*w+x;float gain=1;int value=read(input,index);
            switch(n.id){
                case Effects.PIXEL_DAMAGE:
                    if(hash(seed,x,0)<n.get("columnDensity"))value=hash(seed+71,x,0)<n.get("hotFraction")?black+Math.round((white-black)*n.get("hotValue")):black;
                    else if(hash(seed,x,y+1)<n.get("pixelDensity"))value=hash(seed+71,x,y+1)<n.get("hotFraction")?black+Math.round((white-black)*n.get("hotValue")):black;
                    value+=Math.round((hash(seed^(long)n.get("grainSeed"),x,y)-.5f)*n.get("sensorNoise")*(white-black));break;
                case Effects.EXPOSURE:
                    gain=1-n.get("exposureDepth")*(.5f+.5f*n.get("integration")*(float)Math.sin(y/(float)h*n.get("scanPhase")+n.get("exposurePhase")));
                    value=black+Math.round((value-black)*gain);break;
                case Effects.ROW_ERROR:
                    int row=(int)((y&~1)/(float)h*n.get("rowGroups"));
                    float displacement=hash(seed,row,0)<n.get("weakRows")?(hash(seed+17,row,0)-.5f)*n.get("rowOffset"):0;
                    displacement+=((y&~1)/(float)h-.5f)*n.get("readoutShear");sx+=Math.round(displacement*w/2)*2;
                    value=sx<0||sx>=w?black:read(input,y*w+sx);
                    int start=(int)(n.get("linePosition")*h/2)*2;
                    if(y>=start&&y<start+n.get("lineHeight")*h){int previous=start-2+(y&1);int retained=previous<0||sx<0||sx>=w?black:read(input,previous*w+sx);retained=black+Math.round((retained-black)*n.get("lineRetention"));value=Math.round(value*(1-n.get("lineLoss"))+retained*n.get("lineLoss"));}break;
                case Effects.BIT_ERROR:
                    int block=Math.max(2,(int)n.get("bitBlock"));
                    if(hash(seed^(long)n.event.pattern,x/block,y/Math.max(1,block/2))<n.get("bitProbability"))value^=1<<Math.round(n.get("bitIndex")*(bits-1));break;
                case Effects.ADDRESS_ERROR:
                    int bytes=Math.max(2,(int)n.get("addressRegion")),offset=(int)n.get("byteOffset"),address=index*2;
                    if(offset>0&&hash(seed,address/bytes,0)<n.get("addressProbability")){int src=address+offset;value=src+1<input.length?(input[src]&255)|((input[src+1]&255)<<8):black;}break;
                case Effects.CFA_ERROR:
                    int region=Math.max(2,(int)n.get("cfaRegion"));
                    if(hash(seed,x/region,y/region)<n.get("cfaCoverage")){int phase=(int)n.get("cfaPhase");sx=phase==1?x:x^1;sy=phase==0?y:y^1;if(sx<w&&sy<h)value=read(input,sy*w+sx);}break;
                default:break;
            }
            write(out,index,Math.max(0,Math.min(white,value)));
        }
        return out;
    }
}
