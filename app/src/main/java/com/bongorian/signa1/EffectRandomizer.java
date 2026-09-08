package com.bongorian.signa1;

import java.util.*;

/** Random routes use only the current capture format's available faults. */
final class EffectRandomizer {
    static EffectState chain(EffectState base,int[] available,Random random){
        List<Integer> choices=new ArrayList<>();
        for(int id:available)if(id>Effects.CLEAN&&id<Effects.NAMES.length&&!choices.contains(id))choices.add(id);
        Collections.shuffle(choices,random);
        if(choices.isEmpty())return base.single(Effects.CLEAN);
        int minimum=Math.min(2,choices.size()),maximum=Math.min(5,choices.size());
        int count=minimum+random.nextInt(maximum-minimum+1),mask=0;
        EffectParameters parameters=base.parameters();
        for(int i=0;i<count;i++){
            int id=choices.get(i);mask|=1<<id;parameters=parameters.clearOverrides(id).reseed(id,random.nextLong());
            for(Effects.Control control:Effects.CONTROLS[id])parameters=parameters.with(id,control.key,random.nextFloat());
        }
        return base.edit(true,mask,parameters).amount(.35f+random.nextFloat()*.65f);
    }
}
