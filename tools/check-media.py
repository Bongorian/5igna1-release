#!/usr/bin/env python3
"""Check exported media, including the sensor-clock / muxer-clock regression."""
import argparse
import json
import pathlib
import subprocess
import sys

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--min-fps', type=float, default=20, help='minimum measured cadence (default: 20; set explicitly for 15fps modes or slow emulators)')
parser.add_argument('files', nargs='+')
args = parser.parse_args()
if not 0 <= args.min_fps < 250:
    parser.error('--min-fps must be between 0 and 250')
for name in args.files:
    path = pathlib.Path(name)
    info = json.loads(subprocess.check_output([
        'ffprobe', '-v', 'error', '-show_streams', '-show_format', '-of', 'json', str(path)
    ]))
    video = next(s for s in info['streams'] if s['codec_type'] == 'video')
    assert video['width'] > 0 and video['height'] > 0, video
    if path.suffix.lower() == '.mp4':
        duration = float(info['format']['duration'])
        assert 0 < duration < 120, f'Unexpected duration for short test clip: {duration}'
        assert abs(float(video['duration']) - duration) < .3, 'Video starts far from media origin'
        assert abs(float(video['start_time'])) < .3, 'Camera clock leaked into MP4 timestamps'
        packets = json.loads(subprocess.check_output([
            'ffprobe', '-v', 'error', '-select_streams', 'v:0', '-show_packets',
            '-show_entries', 'packet=dts_time', '-of', 'json', str(path)
        ]))['packets']
        times = [float(p['dts_time']) for p in packets]
        assert all(b > a for a, b in zip(times, times[1:])), 'Non-monotonic video timestamps'
        for audio in (s for s in info['streams'] if s['codec_type'] == 'audio'):
            assert audio['codec_name'] == 'aac'
            assert abs(float(audio['start_time']) - float(video['start_time'])) < .3
            assert abs(float(audio['duration']) - float(video['duration'])) < .5
        fps_num, fps_den = map(float, video['avg_frame_rate'].split('/'))
        fps = fps_num / fps_den
        assert args.min_fps < fps < 250, f'Unexpected frame cadence: {fps}'
        result = subprocess.run([
            'ffmpeg', '-v', 'error', '-xerror', '-i', str(path),
            '-map', '0:v:0', '-enc_time_base', 'demux', '-fps_mode', 'passthrough',
            '-f', 'null', '-'
        ], capture_output=True, text=True)
        assert result.returncode == 0 and not result.stderr, result.stderr
        print(f'PASS {path.name}: {video["width"]}x{video["height"]}, {duration:.3f}s, {fps:.2f}fps, timestamps and decode OK')
    else:
        assert video['codec_name'] == 'mjpeg'
        print(f'PASS {path.name}: JPEG {video["width"]}x{video["height"]}')
