package com.bongorian.signa1;

import org.junit.Test;

/** Independent fixtures exercise both distribution variants with identical expectations. */
public final class CoreBehaviorTest {
    @Test public void adaptivePreviewLoad(){
        AdaptiveLoad load=new AdaptiveLoad();load.sample(0,0,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertEquals(24,load.previewFps);
        int rendered=0;for(int n=1;n<=300;n++){long stamp=n*33_333_333L;if(load.due(stamp)){load.presented(stamp);rendered++;}}org.junit.Assert.assertTrue(rendered>=238&&rendered<=242);
        load.sample(1000,2,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertEquals(12,load.previewFps);load.sample(2000,0,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertEquals(12,load.previewFps);load.sample(17000,0,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertEquals(15,load.previewFps);
        load.sample(18000,0,46,Float.NaN,2_000_000,2,false);org.junit.Assert.assertEquals(6,load.previewFps);org.junit.Assert.assertFalse(load.cooling);
        load.sample(19000,4,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertTrue(load.cooling);load.sample(20000,0,30,Float.NaN,2_000_000,2,false);load.sample(49999,0,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertTrue(load.cooling);load.sample(50000,0,30,Float.NaN,2_000_000,2,false);org.junit.Assert.assertFalse(load.cooling);
        AdaptiveLoad predictive=new AdaptiveLoad();predictive.sample(0,-1,Float.NaN,.9f,2_000_000,2,false);org.junit.Assert.assertEquals(12,predictive.previewFps);
        AdaptiveLoad busy=new AdaptiveLoad();busy.sample(0,0,Float.NaN,Float.NaN,12_000_000,13,false);org.junit.Assert.assertEquals(6,busy.previewFps);AdaptiveLoad slow=new AdaptiveLoad();slow.rendered(70);slow.sample(0,0,Float.NaN,Float.NaN,1_000_000,1,false);org.junit.Assert.assertEquals(8,slow.previewFps);
    }
    @Test public void expertBypassesOnlyAdaptivePolicy(){AdaptiveLoad load=new AdaptiveLoad();load.sample(0,6,60,2,100_000_000,13,true);org.junit.Assert.assertTrue(load.cooling);load.setExpert(true,120);load.rendered(500);load.sample(1,6,60,2,100_000_000,13,true);org.junit.Assert.assertFalse(load.cooling);org.junit.Assert.assertEquals(120,load.cameraFps());load.presented(1_000_000_000);org.junit.Assert.assertTrue(load.due(1_000_000_001));load.setExpert(false,120);load.sample(2,6,60,2,100_000_000,13,true);org.junit.Assert.assertTrue(load.cooling);org.junit.Assert.assertEquals(6,load.previewFps);}
    @Test public void immutableStateAndRawRows() { EffectStateCheck.main(new String[0]); }
    @Test public void pipelineFixtures() { PipelineCheck.main(new String[0]); }
    @Test public void displayedFrameCapture() { FrameHistoryCheck.main(new String[0]); }
    @Test public void liveFaultBehavior() { FaultModelCheck.main(new String[0]); }
    @Test public void advancedPerformanceContracts(){AdvancedPerformanceCheck.main(new String[0]);}
    @Test public void randomChainsRespectFormatAndKeepOriginal(){
        EffectState original=EffectState.defaults().single(Effects.VHS);String saved=original.encode();
        java.util.Random random=new java.util.Random(725);
        java.util.Set<Integer> masks=new java.util.HashSet<>();
        for(int i=0;i<100;i++){
            EffectState generated=EffectRandomizer.chain(original,Effects.choices(false,true),random);
            org.junit.Assert.assertTrue(generated.chained);
            org.junit.Assert.assertTrue(generated.ids().length>=2&&generated.ids().length<=5);
            int previous=0;
            for(int id:generated.ids()){
                org.junit.Assert.assertTrue(Effects.raw(id)&&id>previous);previous=id;
                for(Effects.Control control:Effects.CONTROLS[id]){
                    float value=generated.parameters().get(id,control.key);
                    org.junit.Assert.assertTrue(Float.isFinite(value)&&value>=0&&value<=1);
                }
            }
            org.junit.Assert.assertEquals(generated.encode(),EffectState.decode(generated.encode()).encode());
            masks.add(generated.mask);
        }
        org.junit.Assert.assertTrue(masks.size()>10);
        org.junit.Assert.assertEquals(saved,original.encode());
        org.junit.Assert.assertEquals(EffectRandomizer.chain(original,Effects.ORDER,new java.util.Random(42)).encode(),EffectRandomizer.chain(original,Effects.ORDER,new java.util.Random(42)).encode());
        org.junit.Assert.assertEquals(0,EffectRandomizer.chain(original,new int[]{0},random).mask);
    }
}
