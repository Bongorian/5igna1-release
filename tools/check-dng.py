#!/usr/bin/env python3
"""Validate that LibRaw can open, unpack, and develop a saved DNG."""
import ctypes
import sys
import os
from ctypes.util import find_library
library = os.environ.get('LIBRAW_LIBRARY') or find_library('raw')
if not library:
    sys.exit('LibRaw not found. macOS: brew install libraw; or set LIBRAW_LIBRARY to its library path.')
raw = ctypes.CDLL(library)
raw.libraw_init.argtypes = [ctypes.c_uint]
raw.libraw_init.restype = ctypes.c_void_p
raw.libraw_open_file.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
raw.libraw_open_file.restype = ctypes.c_int
for name in ['libraw_unpack', 'libraw_dcraw_process']:
    getattr(raw, name).argtypes = [ctypes.c_void_p]
    getattr(raw, name).restype = ctypes.c_int
raw.libraw_close.argtypes = [ctypes.c_void_p]
raw.libraw_close.restype = None
for name in sys.argv[1:]:
    context = raw.libraw_init(0)
    if not context:
        raise MemoryError('LibRaw init')
    try:
        assert raw.libraw_open_file(context, name.encode()) == 0, 'DNG open'
        assert raw.libraw_unpack(context) == 0, 'DNG unpack'
        assert raw.libraw_dcraw_process(context) == 0, 'DNG develop'
        print('PASS LibRaw open/unpack/develop:', name)
    finally:
        raw.libraw_close(context)
