#extension GL_OES_EGL_image_external : require
// EFFECT_IDS
precision highp float;
varying vec2 uv;
uniform samplerExternalOES cam;
uniform mat4 st;
uniform int mode;
uniform vec2 sourceSize;
// Compiled mechanism parameters. Only the current fault's named values are bound.
uniform float identitySeed,eventSeed,grainSeed;
uniform float pixelDensity,columnDensity,hotFraction,hotValue,sensorNoise;
uniform float exposureDepth,exposurePhase,scanPhase,integration;
uniform float weakRows,rowGroups,rowOffset,readoutShear,lineLoss,linePosition,lineHeight,lineRetention;
uniform float bitProbability,bitIndex,bitBlock;
uniform float byteOffset,addressRegion,addressProbability;
uniform float cfaCoverage,cfaPhase,cfaRegion,interpolationMix,sampleScale;
uniform float chromaOffset,chromaAngle,chromaBlock;
uniform float paletteMix,palettePhase,paletteCycles;
uniform float quantLevels,blockColumns,blockError,blockOffset;
uniform float streamLoss,streamColumns,concealment;
uniform float tapeBandwidth,trackingOffset,trackingWave,trackingPhase,trackingSlip,tapeDropout,dropoutPosition,tapeNoise;
uniform float scanDepth,scanLines,phosphorMix,convergenceOffset,syncOffset;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
vec3 sampleAt(vec2 p){p=clamp(p,vec2(.00001),vec2(.99999));return texture2D(cam,(st*vec4(p,0.,1.)).xy).rgb;}
// Synthetic RGGB mosaic reconstructed from processed RGB, not access to sensor RAW.
float mosaic(vec2 pixel,vec2 cell){
    vec3 rgb=sampleAt((pixel+.5)*cell);vec2 phase=mod(pixel,2.);
    return phase.y<.5?(phase.x<.5?rgb.r:rgb.g):(phase.x<.5?rgb.g:rgb.b);
}
vec3 interpolateCfa(vec2 pixel,vec2 cell,vec2 offset){
    vec2 phase=mod(pixel+offset,2.);
    float v=mosaic(pixel,cell);
    float horizontal=(mosaic(pixel+vec2(-1.,0.),cell)+mosaic(pixel+vec2(1.,0.),cell))*.5;
    float vertical=(mosaic(pixel+vec2(0.,-1.),cell)+mosaic(pixel+vec2(0.,1.),cell))*.5;
    float diagonal=(mosaic(pixel+vec2(-1.,-1.),cell)+mosaic(pixel+vec2(1.,-1.),cell)+mosaic(pixel+vec2(-1.,1.),cell)+mosaic(pixel+vec2(1.,1.),cell))*.25;
    if(phase.y<.5)return phase.x<.5?vec3(v,(horizontal+vertical)*.5,diagonal):vec3(horizontal,v,vertical);
    return phase.x<.5?vec3(vertical,v,horizontal):vec3(diagonal,(horizontal+vertical)*.5,v);
}
vec3 brokenDemosaic(vec2 pixel,vec2 cell,float seed){
    // Keep the CFA phase correct, but replace edge-aware interpolation with inconsistent,
    // one-sided neighbors. This is an ISP interpolation error, not a CFA phase change.
    vec2 phase=mod(pixel,2.);
    vec2 direction=vec2(hash(floor(pixel/8.)+seed)<.5?-1.:1.,hash(floor(pixel/8.)+seed+37.)<.5?-1.:1.);
    float v=mosaic(pixel,cell),horizontal=mosaic(pixel+vec2(direction.x,0.),cell);
    float vertical=mosaic(pixel+vec2(0.,direction.y),cell),diagonal=mosaic(pixel+direction,cell);
    if(phase.y<.5)return phase.x<.5?vec3(v,horizontal,diagonal):vec3(horizontal,v,vertical);
    return phase.x<.5?vec3(vertical,v,horizontal):vec3(diagonal,vertical,v);
}

vec3 toYuv(vec3 c){return vec3(dot(c,vec3(.299,.587,.114)),dot(c,vec3(-.14713,-.28886,.436)),dot(c,vec3(.615,-.51499,-.10001)));}
vec3 fromYuv(vec3 c){return vec3(c.x+1.13983*c.z,c.x-.39465*c.y-.5806*c.z,c.x+2.03211*c.y);}
void main(){
    vec2 p=uv;vec3 clean=sampleAt(p),c=clean;
    if(mode==FX_PIXEL_DAMAGE){
        vec2 px=floor(p*sourceSize);float col=hash(vec2(px.x,identitySeed)),site=hash(px+identitySeed);
        if(col<columnDensity)c=hash(vec2(px.x,identitySeed+71.))<hotFraction?vec3(hotValue):vec3(0.);
        else if(site<pixelDensity)c=hash(px+identitySeed+71.)<hotFraction?vec3(hotValue):vec3(0.);
        c+=(hash(px+grainSeed)-.5)*sensorNoise;
    }else if(mode==FX_EXPOSURE){
        c=clean*(1.-exposureDepth*(.5+.5*integration*sin(p.y*scanPhase+exposurePhase)));
    }else if(mode==FX_ROW_ERROR){
        float row=floor(p.y*rowGroups);
        if(hash(vec2(row,identitySeed))<weakRows)p.x+=(hash(vec2(row,identitySeed+17.))-.5)*rowOffset;
        p.x+=(p.y-.5)*readoutShear;
        // Pair-aligned replay models row readout, not CFA phase mistakes.
        float start=floor(linePosition*sourceSize.y/2.)*2.;
        if(p.y>=start/sourceSize.y&&p.y<start/sourceSize.y+lineHeight&&lineLoss>0.){
            float previous=start-2.+mod(floor(p.y*sourceSize.y),2.);
            vec3 retained=previous<0.?vec3(0.):sampleAt(vec2(p.x,(previous+.5)/sourceSize.y))*lineRetention;
            c=mix(sampleAt(p),retained,lineLoss);
        }else c=sampleAt(p);
        if(p.x<0.||p.x>1.)c=vec3(0.);
    }else if(mode==FX_BIT_ERROR){
        vec2 block=floor(p*sourceSize/vec2(bitBlock,max(1.,bitBlock*.5)));
        if(hash(block+eventSeed)<bitProbability){vec3 bytes=floor(clean*255.);float bit=exp2(floor(bitIndex*7.+.5));vec3 present=mod(floor(bytes/bit),2.);c=(bytes+bit*(1.-2.*present))/255.;}
    }else if(mode==FX_ADDRESS_ERROR){
        // Representation is RGB8 interleaved bytes. No encoded bitstream is claimed here.
        vec2 size=max(vec2(1.),floor(sourceSize)),pixel=min(floor(p*size),size-1.);float index=pixel.x*3.;
        if(byteOffset>0.&&hash(vec2(floor(index/addressRegion),pixel.y+identitySeed))<addressProbability){
            vec3 shifted=vec3(0.);
            for(int channel=0;channel<3;channel++){
                float address=index+float(channel)+byteOffset,linear=floor(address/3.);
                vec2 q=vec2(mod(linear,size.x),pixel.y+floor(linear/size.x));vec3 value=q.y<size.y?sampleAt((q+.5)/size):vec3(0.);
                float component=mod(address,3.),v=component<.5?value.r:component<1.5?value.g:value.b;
                if(channel==0)shifted.r=v;else if(channel==1)shifted.g=v;else shifted.b=v;
            }c=shifted;
        }
    }else if(mode==FX_CFA_ERROR){
        vec2 pixel=floor(p*sourceSize);
        if(hash(floor(pixel/cfaRegion)+identitySeed)<cfaCoverage){vec2 offset=cfaPhase<.5?vec2(1.,0.):cfaPhase<1.5?vec2(0.,1.):vec2(1.,1.);c=interpolateCfa(pixel,1./sourceSize,offset);}
    }else if(mode==FX_DEMOSAIC_ERROR){
        vec2 cell=vec2(sampleScale)/sourceSize,pixel=floor(p/cell);c=mix(clean,brokenDemosaic(pixel,cell,identitySeed),interpolationMix);
    }else if(mode==FX_CHROMA_ERROR){
        vec2 d=vec2(cos(chromaAngle),sin(chromaAngle))*chromaOffset;
        vec2 grid=max(vec2(1.),sourceSize/vec2(chromaBlock,max(1.,chromaBlock*.5)));
        vec2 samplePos=(floor((p+d)*grid)+.5)/grid;
        vec3 chroma=toYuv(sampleAt(samplePos));chroma.x=toYuv(clean).x;c=fromYuv(chroma);
    }else if(mode==FX_COLOR_MAP){
        float l=dot(clean,vec3(.299,.587,.114));vec3 palette=.5+.5*cos(6.28318*(l*paletteCycles+vec3(0.,.33,.67)+palettePhase));c=mix(clean,palette,paletteMix);
    }else if(mode==FX_BLOCK_ERROR){
        // A decoded-block approximation: wrong block address and quantization, never packet editing.
        vec2 grid=vec2(blockColumns,blockColumns*sourceSize.y/sourceSize.x),block=floor(p*grid);
        if(hash(block+eventSeed)<blockError)p.x=fract(p.x+floor(blockOffset*grid.x)/grid.x);
        c=sampleAt(p);float levels=max(4.,quantLevels*(.75+.25*hash(block+identitySeed)));
        // Exactly bypass quantization at the neutral control value.
        if(quantLevels<256.)c=floor(c*levels+.5)/levels;
    }else if(mode==FX_STREAM_ERROR){
        vec2 grid=vec2(streamColumns,streamColumns*sourceSize.y/sourceSize.x),region=floor(p*grid);
        if(hash(region+eventSeed)<streamLoss){vec2 previous=p-vec2(0.,1./grid.y);c=previous.y<0.?vec3(0.):sampleAt(previous)*concealment;}
    }else if(mode==FX_VHS){
        p.x+=trackingOffset+sin(p.y*70.+trackingPhase)*trackingWave;
        if(p.y>dropoutPosition)p.x+=trackingSlip;
        float d=tapeBandwidth*.006;vec3 blurred=(sampleAt(p-vec2(d,0.))+2.*sampleAt(p)+sampleAt(p+vec2(d,0.)))*.25;
        vec3 yc=toYuv(blurred),chroma=toYuv(sampleAt(p-vec2(d*3.,0.)));yc.yz=mix(yc.yz,chroma.yz,tapeBandwidth);c=fromYuv(yc);
        if(abs(p.y-dropoutPosition)<.004+tapeDropout*.03)c=mix(c,vec3(hash(floor(p*sourceSize)+eventSeed)),tapeDropout);
        c+=(hash(floor(p*sourceSize)+grainSeed)-.5)*tapeNoise;
    }else if(mode==FX_CRT){
        p.y=fract(p.y+syncOffset);vec2 d=vec2(convergenceOffset,0.);
        c=vec3(sampleAt(p+d).r,sampleAt(p).g,sampleAt(p-d).b);
        float l=dot(c,vec3(.299,.587,.114));c=mix(c,vec3(.65,1.,.76)*l,phosphorMix);
        c*=1.-scanDepth*(.5+.5*sin(uv.y*scanLines*6.28318));
    }
    gl_FragColor=vec4(clamp(c,0.,1.),1.);
}
