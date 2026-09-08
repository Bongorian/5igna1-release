package com.bongorian.signa1;

import java.util.*;

/** A committed route and its controls. Rendering never mutates settings. */
final class EffectState {
    private static final int VALID_MASK=((1<<Effects.NAMES.length)-1)&~1;
    final boolean chained;final int mask;final float amount;private final EffectParameters parameters;
    private EffectState(boolean chained,int mask,float amount,EffectParameters parameters){
        this.mask=mask&VALID_MASK;this.chained=chained&&this.mask!=0;
        if(!this.chained&&Integer.bitCount(this.mask)>1)throw new IllegalArgumentException("Single fault has multiple stages");
        this.amount=EffectParameters.unit(amount);this.parameters=parameters==null?EffectParameters.defaults():parameters;
    }
    static EffectState defaults(){return new EffectState(false,0,.55f,null);}
    static EffectState create(int selected,int mask,float level,EffectParameters parameters){int valid=mask&VALID_MASK;return new EffectState(valid!=0,valid!=0?valid:bit(selected),level,parameters);}
    private static int bit(int id){return id>0&&id<Effects.NAMES.length?1<<id:0;}
    int selected(){for(int id:Effects.ORDER)if(enabled(id))return id;return Effects.CLEAN;}
    int[] ids(){return Effects.ordered(mask,false);}
    boolean enabled(int id){return (mask&bit(id))!=0;}
    EffectParameters parameters(){return parameters;}
    EffectState single(int id){return new EffectState(false,bit(id),amount,parameters);}
    EffectState chain(int mask){return new EffectState(true,mask,amount,parameters);}
    EffectState amount(float value){return new EffectState(chained,mask,value,parameters);}
    EffectState edit(boolean chain,int mask,EffectParameters p){return new EffectState(chain,mask,amount,p);}
    EffectState forContext(boolean video,int format){
        if(!video&&format==1)return this;int allowed=0;for(int id:ids())if(Effects.available(id,video,!video&&format==2))allowed|=bit(id);
        return allowed==mask?this:new EffectState(chained,allowed,amount,parameters);
    }
    Frame snapshot(boolean video,int format){EffectState valid=forContext(video,format);return new Frame(!video&&format==1?new int[0]:valid.ids(),amount,parameters,0,0,Collections.emptyList());}
    static final class Frame {
        private final int[] route;final float amount;final EffectParameters parameters;
        final long cameraNs;final double time;final List<FaultNode> nodes;
        Frame(int[] ids,float amount,EffectParameters parameters,long cameraNs,double time,List<FaultNode> nodes){
            int previous=0;for(int id:ids){if(id<=previous||id>=Effects.NAMES.length)throw new IllegalArgumentException("Non-causal fault route");previous=id;}
            previous=0;for(FaultNode n:nodes){if(n.id<=previous||Arrays.binarySearch(ids,n.id)<0)throw new IllegalArgumentException("Fault node outside route");previous=n.id;}
            this.route=ids.clone();this.amount=amount;this.parameters=parameters;this.cameraNs=cameraNs;this.time=time;this.nodes=Collections.unmodifiableList(new ArrayList<>(nodes));
        }
        int[] ids(){return route.clone();}
        // Recordable causal prefix; profile/representation after the chosen point is explicit.
        Frame through(Effects.Point point){int[] prefix=Arrays.stream(route).filter(id->Effects.point(id).ordinal()<=point.ordinal()).toArray();List<FaultNode> selected=new ArrayList<>();for(FaultNode n:nodes)if(Effects.point(n.id).ordinal()<=point.ordinal())selected.add(n);return new Frame(prefix,amount,parameters,cameraNs,time,selected);}
        String describe(){StringBuilder s=new StringBuilder("cameraNs=").append(cameraNs).append(" t=").append(time).append(" LEVEL=").append(amount).append(parameters.describe(route));for(FaultNode n:nodes)s.append(" | ").append(n.describe());return s.toString();}
    }
    String encode(){return "3|"+(chained?1:0)+"|"+mask+"|"+amount+"|"+parameters.encode();}
    static EffectState decode(String text){String[] p=text.split("\\|",-1);if(p.length!=5||!p[0].equals("3")||!(p[1].equals("0")||p[1].equals("1")))throw new IllegalArgumentException("Fault schema");int mask=Integer.parseInt(p[2]);float level=Float.parseFloat(p[3]);if((mask&~VALID_MASK)!=0||!Float.isFinite(level)||level<0||level>1)throw new IllegalArgumentException("Fault settings");return new EffectState(p[1].equals("1"),mask,level,EffectParameters.decode(p[4]));}
}
