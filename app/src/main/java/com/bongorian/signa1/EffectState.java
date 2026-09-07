package com.bongorian.signa1;

import java.util.Arrays;

/** One immutable effect transaction. UI, GL and capture never observe partial edits. */
final class EffectState {
    private static final int VALID_MASK=((1<<Effects.NAMES.length)-1)&~(1<<Effects.CLEAN);
    final boolean chained;
    final int mask;
    final float amount;
    private final float[] parameters;

    private EffectState(boolean chained,int mask,float amount,float[] parameters){
        this.mask=mask&VALID_MASK;
        this.chained=chained&&this.mask!=0;
        if(!this.chained&&Integer.bitCount(this.mask)>1)throw new IllegalArgumentException("Single effect has multiple stages");
        this.amount=EffectParameters.unit(amount);
        float[] values=EffectParameters.defaults();
        if(parameters!=null)for(int i=0;i<Math.min(values.length,parameters.length);i++)values[i]=Float.isFinite(parameters[i])?EffectParameters.unit(parameters[i]):values[i];
        this.parameters=values;
    }
    static EffectState defaults(){return new EffectState(false,0,.55f,null);}
    static EffectState create(int selected,int chainMask,float amount,float[] parameters){
        int valid=chainMask&VALID_MASK;
        return new EffectState(valid!=0,valid!=0?valid:bit(selected),amount,parameters);
    }
    private static int bit(int id){return id>=0&&id<Effects.NAMES.length&&id!=Effects.CLEAN?1<<id:0;}
    int selected(){for(int id:Effects.ORDER)if((mask&bit(id))!=0)return id;return Effects.CLEAN;}
    int[] ids(){return Arrays.stream(Effects.ORDER).filter(id->(mask&bit(id))!=0).toArray();}
    boolean enabled(int id){return (mask&bit(id))!=0;}
    float[] parameters(){return parameters.clone();}
    EffectState single(int id){return new EffectState(false,bit(id),amount,parameters);}
    EffectState chain(int mask){return new EffectState(true,mask,amount,parameters);}
    EffectState amount(float value){return new EffectState(chained,mask,value,parameters);}
    EffectState edit(boolean chain,int mask,float[] values){return new EffectState(chain,mask,amount,values);}
    EffectState random(boolean chain,int mask,float amount,float[] values){return new EffectState(chain,mask,amount,values);}

    /** Unsupported stages are removed, not kept in a hidden secondary selection. RAW original is a bypass. */
    EffectState forContext(boolean video,int photoFormat){
        if(!video&&photoFormat==1)return this;
        int allowed=0;for(int id:ids())if(Effects.available(id,video,!video&&photoFormat==2))allowed|=bit(id);
        return allowed==mask?this:new EffectState(chained,allowed,amount,parameters);
    }
    Frame snapshot(boolean video,int photoFormat){
        EffectState valid=forContext(video,photoFormat);
        return new Frame(!video&&photoFormat==1?new int[0]:valid.ids(),valid.amount,valid.parameters.clone());
    }
    static final class Frame {
        final int[] ids;final float amount;final float[] parameters,live;final float time;
        private Frame(int[] ids,float amount,float[] parameters){this(ids,amount,parameters,null,Float.NaN);}
        Frame(int[] ids,float amount,float[] parameters,float[] live,float time){this.ids=ids;this.amount=amount;this.parameters=parameters;this.live=live;this.time=time;}
    }
    String encode(){StringBuilder s=new StringBuilder("2|").append(chained?1:0).append('|').append(mask).append('|').append(amount);for(float value:parameters)s.append('|').append(value);return s.toString();}
    static EffectState decode(String encoded){
        String[] pieces=encoded.split("\\|",-1);int length=Effects.NAMES.length*EffectParameters.STRIDE;
        if(pieces.length!=4+length||!pieces[0].equals("2")||!(pieces[1].equals("0")||pieces[1].equals("1")))throw new IllegalArgumentException("Effect state schema");
        float[] values=EffectParameters.defaults();for(int i=0;i<length;i++)values[i]=Float.parseFloat(pieces[i+4]);
        return new EffectState(pieces[1].equals("1"),Integer.parseInt(pieces[2]),Float.parseFloat(pieces[3]),values);
    }
}
