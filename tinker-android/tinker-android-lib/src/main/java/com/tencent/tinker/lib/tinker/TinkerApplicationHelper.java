/*
 * Copyright (C) 2016 Tencent WeChat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tinker.lib.tinker;

import android.content.Intent;

import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.loader.TinkerRuntimeException;
import com.tencent.tinker.loader.app.TinkerApplication;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import com.tencent.tinker.loader.shareutil.ShareIntentUtil;
import com.tencent.tinker.loader.shareutil.SharePatchFileUtil;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import java.io.File;
import java.util.HashMap;

/**
 * sometimes, you may want to install tinker later, or never install tinker in some process.
 * you can use {@code TinkerApplicationHelper} API to get the tinker status!
 * Created by shwenzhang on 16/6/28.
 */
public class TinkerApplicationHelper {
    private static final String TAG = "TinkerApplicationHelper";

    /**
     * they can use without Tinker is installed!
     * same as {@code Tinker.isEnabled}
     *
     * @return
     */
    public static boolean isTinkerEnableAll(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }
        int tinkerFlags = tinkerApplication.getTinkerFlags();
        return tinkerFlags == TinkerApplication.TINKER_ENABLE_ALL;
    }

    /**
     * same as {@code Tinker.isEnabledForDex}
     *
     * @param tinkerApplication
     * @return
     */
    public static boolean isTinkerEnableForDex(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }
        int tinkerFlags = tinkerApplication.getTinkerFlags();
        return tinkerFlags == TinkerApplication.TINKER_DEX_ONLY || tinkerFlags == TinkerApplication.TINKER_ENABLE_ALL;
    }

    /**
     * same as {@code Tinker.isEnabledForNativeLib}
     *
     * @param tinkerApplication
     * @return
     */
    public static boolean isTinkerEnableForNativeLib(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }
        int tinkerFlags = tinkerApplication.getTinkerFlags();
        return tinkerFlags == TinkerApplication.TINKER_LIBRARY_ONLY || tinkerFlags == TinkerApplication.TINKER_ENABLE_ALL;
    }

    /**
     * same as {@code Tinker.getPatchDirectory}
     *
     * @param tinkerApplication
     * @return
     */
    public static File getTinkerPatchDirectory(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }

        return SharePatchFileUtil.getPatchDirectory(tinkerApplication);
    }

    /**
     * whether tinker is success loaded
     * same as {@code Tinker.isTinkerLoaded}
     *
     * @param tinkerApplication
     * @return
     */
    public static boolean isTinkerLoadSuccess(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }

        Intent tinkerResultIntent = tinkerApplication.getTinkerResultIntent();

        if (tinkerResultIntent == null) {
            return false;
        }
        int loadCode = ShareIntentUtil.getIntentReturnCode(tinkerResultIntent);

        return (loadCode == ShareConstants.ERROR_LOAD_OK);
    }

    /**
     * you can use this api to get load dexes before tinker is installed
     * same as {@code Tinker.getTinkerLoadResultIfPresent.dexes}
     *
     * @return
     */
    public static HashMap<String, String> getLoadDexesAndMd5(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }

        Intent tinkerResultIntent = tinkerApplication.getTinkerResultIntent();

        if (tinkerResultIntent == null) {
            return null;
        }
        int loadCode = ShareIntentUtil.getIntentReturnCode(tinkerResultIntent);

        if (loadCode == ShareConstants.ERROR_LOAD_OK) {
            return ShareIntentUtil.getIntentPatchDexPaths(tinkerResultIntent);
        }
        return null;
    }


    /**
     * you can use this api to get load libs before tinker is installed
     * same as {@code Tinker.getTinkerLoadResultIfPresent.libs}
     *
     * @return
     */
    public static HashMap<String, String> getLoadLibraryAndMd5(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }

        Intent tinkerResultIntent = tinkerApplication.getTinkerResultIntent();

        if (tinkerResultIntent == null) {
            return null;
        }
        int loadCode = ShareIntentUtil.getIntentReturnCode(tinkerResultIntent);

        if (loadCode == ShareConstants.ERROR_LOAD_OK) {
            return ShareIntentUtil.getIntentPatchLibsPaths(tinkerResultIntent);
        }
        return null;
    }

    /**
     * you can use this api to get tinker package configs before tinker is installed
     * same as {@code Tinker.getTinkerLoadResultIfPresent.packageConfig}
     *
     * @return
     */
    public static HashMap<String, String> getPackageConfigs(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }

        Intent tinkerResultIntent = tinkerApplication.getTinkerResultIntent();

        if (tinkerResultIntent == null) {
            return null;
        }
        int loadCode = ShareIntentUtil.getIntentReturnCode(tinkerResultIntent);

        if (loadCode == ShareConstants.ERROR_LOAD_OK) {
            return ShareIntentUtil.getIntentPackageConfig(tinkerResultIntent);
        }
        return null;
    }

    /**
     * you can use this api to get tinker current version before tinker is installed
     *
     * @return
     */
    public static String getCurrentVersion(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }

        Intent tinkerResultIntent = tinkerApplication.getTinkerResultIntent();

        if (tinkerResultIntent == null) {
            return null;
        }
        final String oldVersion = ShareIntentUtil.getStringExtra(tinkerResultIntent, ShareIntentUtil.INTENT_PATCH_OLD_VERSION);
        final String newVersion = ShareIntentUtil.getStringExtra(tinkerResultIntent, ShareIntentUtil.INTENT_PATCH_NEW_VERSION);
        final boolean isMainProcess = ShareTinkerInternals.isInMainProcess(tinkerApplication);
        if (oldVersion != null && newVersion != null) {
            if (isMainProcess) {
                return newVersion;
            } else {
                return oldVersion;
            }
        }
        return null;
    }

    /**
     * clean all patch files without install tinker
     * same as {@code Tinker.cleanPatch}
     *
     * @param tinkerApplication
     */
    public static void cleanPatch(TinkerApplication tinkerApplication) {
        if (tinkerApplication == null) {
            throw new TinkerRuntimeException("tinkerApplication is null");
        }
        SharePatchFileUtil.deleteDir(SharePatchFileUtil.getPatchDirectory(tinkerApplication));
    }

    /**
     * only support auto load lib/armeabi-v7a library from patch.
     * in some process, you may not want to install tinker
     * and you can load patch dex and library without install tinker!
     * }
     */
    public static void loadArmV7aLibrary(TinkerApplication tinkerApplication, String libName) {
        if (libName == null || libName.isEmpty() || tinkerApplication == null) {
            throw new TinkerRuntimeException("libName or context is null!");
        }

        if (TinkerApplicationHelper.isTinkerEnableForNativeLib(tinkerApplication)) {
            if (TinkerApplicationHelper.loadLibraryFromTinker(tinkerApplication, "lib/armeabi-v7a", libName)) {
                return;
            }

        }
        System.loadLibrary(libName);
    }


    /**
     * only support auto load lib/armeabi library from patch.
     * in some process, you may not want to install tinker
     * and you can load patch dex and library without install tinker!
     */
    public static void loadArmLibrary(TinkerApplication tinkerApplication, String libName) {
        if (libName == null || libName.isEmpty() || tinkerApplication == null) {
            throw new TinkerRuntimeException("libName or context is null!");
        }

        if (TinkerApplicationHelper.isTinkerEnableForNativeLib(tinkerApplication)) {
            if (TinkerApplicationHelper.loadLibraryFromTinker(tinkerApplication, "lib/armeabi", libName)) {
                return;
            }

        }
        System.loadLibrary(libName);
    }

    /**
     * you can use these api to load tinker library without tinker is installed!
     * same as {@code TinkerInstaller#loadLibraryFromTinker}
     *
     * @param tinkerApplication
     * @param relativePath
     * @param libname
     * @return
     * @throws UnsatisfiedLinkError
     */
    public static boolean loadLibraryFromTinker(TinkerApplication tinkerApplication, String relativePath, String libname) throws UnsatisfiedLinkError {
        libname = libname.startsWith("lib") ? libname : "lib" + libname;
        libname = libname.endsWith(".so") ? libname : libname + ".so";
        String relativeLibPath = relativePath + "/" + libname;

        //TODO we should add cpu abi, and the real path later
        if (TinkerApplicationHelper.isTinkerEnableForNativeLib(tinkerApplication)
            && TinkerApplicationHelper.isTinkerLoadSuccess(tinkerApplication)) {
            HashMap<String, String> loadLibraries = TinkerApplicationHelper.getLoadLibraryAndMd5(tinkerApplication);
            if (loadLibraries != null) {
                String currentVersion = TinkerApplicationHelper.getCurrentVersion(tinkerApplication);
                if (ShareTinkerInternals.isNullOrNil(currentVersion)) {
                    return false;
                }
                File patchDirectory = SharePatchFileUtil.getPatchDirectory(tinkerApplication);
                File patchVersionDirectory = new File(patchDirectory.getAbsolutePath() + "/" + SharePatchFileUtil.getPatchVersionDirectory(currentVersion));
                String libPrePath = patchVersionDirectory.getAbsolutePath() + "/" + ShareConstants.SO_PATH;

                for (String name : loadLibraries.keySet()) {
                    if (name.equals(relativeLibPath)) {
                        String patchLibraryPath = libPrePath + "/" + name;
                        File library = new File(patchLibraryPath);
                        if (library.exists()) {
                            //whether we check md5 when load
                            boolean verifyMd5 = tinkerApplication.getTinkerLoadVerifyFlag();
                            if (verifyMd5 && !SharePatchFileUtil.verifyFileMd5(library, loadLibraries.get(name))) {
                                //do not report, because tinker is not install
                                TinkerLog.i(TAG, "loadLibraryFromTinker md5mismatch fail:" + patchLibraryPath);
                            } else {
                                System.load(patchLibraryPath);
                                TinkerLog.i(TAG, "loadLibraryFromTinker success:" + patchLibraryPath);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}