# Wine patches for the ATFCORE Windows add-on

The add-on runs Wine 10.10. `assets/rootfs_patches.tzst` carries rebuilt modules
(`opt/wine/lib/wine/x86_64-unix/win32u.so`, `x86_64-windows/win32u.dll`, `x86_64-windows/user32.dll`)
built from wine-10.10 with these patches.

- `0001-mouse-in-pointer-emulation.patch`: EnableMouseInPointer + WM_POINTER* emulation of mouse
  input and GetPointerInfo (Wine bug 53847, patch by Rémi Bernon updated by Aleksey for 11.1;
  applies to 10.10). Unity 6 players import EnableMouseInPointer and, once it exists, read the mouse
  only through pointer messages: without this every click was ignored.

Build (x86_64 host, llvm-mingw for the PE side):

    ./configure --enable-win64 --disable-tests --with-mingw=clang --without-x ... (vulkan, freetype,
    fontconfig enabled: win32u loads them)
    make __tools__ ; make dlls/win32u/all dlls/user32/all

The .so must not need symbols newer than GLIBC_2.35 (Box64 0.4.4 wraps the guest libc).
Bump `versionCode` when the modules change: containers re-extract rootfs_patches on a new app version.
