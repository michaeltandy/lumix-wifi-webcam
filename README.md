# Lumix wifi preview stream to webcam hack

Some Panasonic Lumix cameras support Panasonic's 'Image App' which streams an mjpeg stream over wifi.

This program connects to a camera that supports that, and forwards the mjpeg stream into `ffmpeg` then into `v4l2loopback` creating a webcam. I've tested this with my GX80.

Inspired by [lumix-link-desktop](https://github.com/peci1/lumix-link-desktop/releases) although this program shares no code with that one.
