package com.cgl.ifind.shizuku;

import android.os.Bundle;

interface IShizukuCommandService {
    void destroy() = 16777114;
    Bundle runCommand(String command) = 1;
}
