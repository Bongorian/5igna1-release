#extension GL_OES_EGL_image_external : require
// EFFECT_IDS
precision highp float;
varying vec2 uv;
uniform samplerExternalOES cam;
uniform mat4 st;
uniform float t,a;
uniform int mode;
uniform vec2 sourceSize;
// Shape, character, and a stable spatial seed. All controls use 0..1.
uniform vec3 detail;
// Measured-input snapshot: enabled, signed displacement / phase, transient / skew, integration.
uniform vec4 live;
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
void main(){
    vec2 p=uv;float power=clamp(a,0.,1.);float x=detail.x,y=detail.y,seed=detail.z*997.;
    vec3 clean=sampleAt(p),c=clean;
    if(mode==FX_CLEAN||power==0.){gl_FragColor=vec4(clean,1.);return;}
    float tick=floor(t*14.)+seed;
    float band=hash(vec2(floor(p.y*60.),tick));
    if(mode==FX_CHROMA){
        // Spatial displacement has headroom; blend ratios remain bounded.
        p.x+=step(1.-power*.45,band)*(band-.5)*mix(.01,.65,y)*power;
        vec2 d=vec2(cos(x*1.570796),sin(x*1.570796))*power*.09;
        c=vec3(sampleAt(p+d).r,sampleAt(p).g,sampleAt(p-d).b);
    }else if(mode==FX_VHS){
        if(live.x>0.)p.x+=power*(live.y+x*(sin(p.y*70.+t*9.)*.004+step(.8,band)*.06*live.z));
        else p.x+=power*x*(sin(p.y*70.+t*9.)*.015+step(.8,band)*.14);
        float d=.018*power;c=vec3(sampleAt(p+vec2(d,0)).r,sampleAt(p).g,sampleAt(p-vec2(d,0)).b);
        float l=dot(c,vec3(.299,.587,.114));c=mix(c,vec3(l),.25*power);
        c*=1.-.2*power*(.5+.5*sin(uv.y*1300.));c+=(hash(uv+fract(t)+seed)-.5)*.45*power*y;
    }else if(mode==FX_CORRUPT){
        vec2 grid=mix(vec2(80.,120.),vec2(8.,12.),x);vec2 block=floor(p*grid);float n=hash(block+tick);
        if(n<power*.8){p.x=fract(p.x+(hash(block+tick+3.)-.5)*.7*power);p=(floor(p*grid*4.)+.5)/(grid*4.);}
        c=sampleAt(p);if(n<power*y*.7)c=c.gbr;
        float levels=mix(64.,4.,power*y);c=mix(c,floor(c*levels)/levels,power);
    }else if(mode==FX_SPECTRUM){
        float l=dot(clean,vec3(.299,.587,.114));vec3 palette=.5+.5*cos(6.28318*(l*mix(.5,4.,x)+vec3(0.,.33,.67)+y));
        c=mix(clean,palette,power);
    }else if(mode==FX_TERMINAL){
        float l=dot(clean,vec3(.299,.587,.114));c=mix(clean,vec3(.65,1.,.76)*smoothstep(.12,.85,l),power);
        c*=1.-power*.45*(.5+.5*sin(uv.y*mix(200.,2000.,x)));c+=(hash(uv+fract(t)+seed)-.5)*power*y*.3;
    }else if(mode==FX_ROW_SHIFT){
        // Stable readout bands. Missing edge samples stay black instead of wrapping.
        float rows=floor(mix(240.,12.,x));float row=floor(p.y*rows);
        if(hash(vec2(row,seed))<power){float shift=(hash(vec2(row,seed+17.))-.5)*y*.6*power;p.x+=shift;}
        if(live.x>0.)p.x+=(uv.y-.5)*live.y*power;
        c=p.x<0.||p.x>1.?vec3(0.):sampleAt(p);
    }else if(mode==FX_BIT_ROT){
        float scale=mix(2.,128.,y);vec2 block=floor(p*sourceSize/vec2(scale,scale*.5));float n=hash(block+tick);
        if(n<power*.8){vec3 bytes=floor(clean*255.);float bit=exp2(floor(x*7.+.5));vec3 present=mod(floor(bytes/bit),2.);c=(bytes+bit*(1.-2.*present))/255.;}
    }else if(mode==FX_CFA_TEAR){
        float scale=mix(8.,256.,x);vec2 block=floor(p*sourceSize/vec2(scale,scale*.5));
        if(hash(block+tick)<power*.88){vec2 px=floor(p*sourceSize);vec2 shift=y<.25?vec2(1.,0.):y<.75?vec2(0.,1.):vec2(1.,1.);vec3 other=sampleAt(p+shift/sourceSize);c=mix(other.gbr,other.brg,mod(px.x+px.y,2.));}
    }else if(mode==FX_SENSOR_FAIL){
        vec2 px=floor(p*sourceSize);float col=hash(vec2(px.x,seed));float point=hash(px+seed);
        if(col<power*.06*x)c=hash(vec2(px.x,seed+71.))<y?vec3(1.):vec3(0.);
        else if(point<power*.006*(1.-x))c=hash(px+seed+71.)<y?vec3(1.):vec3(0.);
    }else if(mode==FX_EXPOSURE_BAND){
        // Exposure loss is bounded attenuation; 100% never adds unbounded light.
        float wave=.5+.5*sin(p.y*mix(12.,180.,x)+t*mix(0.,50.,y)+seed);
        if(live.x>0.)wave=.5+.5*live.w*sin(p.y*live.z+live.y);
        c=clean*(1.-power*wave);
    }else if(mode==FX_CHROMA_LOSS){
        vec2 block=vec2(mix(2.,128.,x),mix(1.,64.,y));vec2 grid=max(vec2(2.),sourceSize/block);
        vec3 coarse=sampleAt((floor(p*grid)+.5)/grid);float l=dot(clean,vec3(.299,.587,.114)),cl=dot(coarse,vec3(.299,.587,.114));
        c=mix(clean,coarse+vec3(l-cl),power);
    }else if(mode==FX_PACKET_LOSS){
        vec2 grid=mix(vec2(64.,96.),vec2(8.,12.),x);vec2 packet=floor(p*grid);float n=hash(packet+tick);
        if(n<power*.75){vec2 q=p;q.y=clamp(q.y-(1.+floor(hash(packet+7.)*3.))/grid.y,0.,1.);c=hash(packet+tick+47.)<y?sampleAt(q):vec3(0.);}
    }else if(mode==FX_DATA_SHIFT){
        // RGB8 interleaved byte-stream approximation of RAW16 boundary corruption.
        float offset=floor(x*31.+.5),block=2.*(2.+floor(y*254.+.5));
        vec2 size=max(vec2(1.),floor(sourceSize)),pixel=min(floor(p*size),size-1.);
        // Keep byte arithmetic row-local: full-resolution linear indices can exceed float precision.
        float index=pixel.x*3.;
        if(offset>0.&&hash(vec2(floor(index/block),pixel.y+seed))<power){
            vec3 shifted=vec3(0.);
            for(int channel=0;channel<3;channel++){
                float address=index+float(channel)+offset,linear=floor(address/3.);
                vec2 q=vec2(mod(linear,size.x),pixel.y+floor(linear/size.x));
                vec3 value=q.y<size.y?sampleAt((q+.5)/size):vec3(0.);
                float component=mod(address,3.);float v=component<.5?value.r:component<1.5?value.g:value.b;
                if(channel==0)shifted.r=v;else if(channel==1)shifted.g=v;else shifted.b=v;
            }
            c=shifted;
        }
    }else if(mode==FX_LINE_LOSS){
        float height=2.*(1.+floor(x*63.+.5)),row=floor(p.y*sourceSize.y),group=floor(row/height);
        if(hash(vec2(group,seed))<power){
            float previous=group*height-2.+mod(row,2.);
            c=previous<0.?vec3(0.):sampleAt(vec2(p.x,(previous+.5)/sourceSize.y))*y;
        }
    }else if(mode==FX_CFA_OFFSET){
        float scale=2.*(1.+floor(y*127.+.5));vec2 pixel=floor(p*sourceSize);
        if(hash(floor(pixel/scale)+seed)<power){
            float phase=floor(x*2.+.5);vec2 offset=phase<.5?vec2(1.,0.):phase<1.5?vec2(0.,1.):vec2(1.,1.);
            c=interpolateCfa(pixel,1./sourceSize,offset);
        }
    }else if(mode==FX_DEMOSAIC){
        float scale=1.+floor(x*15.+.5);vec2 cell=vec2(scale)/sourceSize,pixel=floor(p/cell);
        vec3 broken=brokenDemosaic(pixel,cell,seed);
        c=mix(clean,broken,power*y);

    }
    gl_FragColor=vec4(clamp(c,0.,1.),1.);
}
