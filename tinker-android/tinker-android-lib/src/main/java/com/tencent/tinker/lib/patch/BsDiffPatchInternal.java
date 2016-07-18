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

package com.tencent.tinker.lib.patch;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;

import com.tencent.tinker.bsdiff.BSPatch;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.loader.TinkerRuntimeException;
import com.tencent.tinker.loader.shareutil.ShareBsDiffPatchInfo;
import com.tencent.tinker.loader.shareutil.SharePatchFileUtil;
import com.tencent.tinker.loader.shareutil.ShareSecurityCheck;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import java.io.File;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by shwenzhang on 16/3/21.
 */
public class BsDiffPatchInternal extends BasePatchInternal {
    private static final String TAG = "BsDiffPatchInternal";

    protected static boolean tryRecoverLibraryFiles(Tinker manager, ShareSecurityCheck checker, Context context,
                                                    String patchVersionDirectory, File patchFile, boolean isUpgradePatch) {

        if (!manager.isEnabledForNativeLib()) {
            TinkerLog.w(TAG, "patch recover, library is not enabled");
            return true;
        }
        String libMeta = checker.getMetaContentMap().get(SO_META_FILE);

        if (libMeta == null) {
            TinkerLog.w(TAG, "patch recover, library is not contained");
            return true;
        }
        long begin = SystemClock.elapsedRealtime();
        boolean result = patchLibraryExtractViaBsDiff(context, patchVersionDirectory, libMeta, patchFile, isUpgradePatch);
        long cost = SystemClock.elapsedRealtime() - begin;
        TinkerLog.i(TAG, "recover lib result:%b, cost:%d, isUpgradePatch:%b", result, cost, isUpgradePatch);
        return result;
    }


    private static boolean patchLibraryExtractViaBsDiff(Context context, String patchVersionDirectory, String meta, File patchFile, boolean isUpgradePatch) {
        String dir = patchVersionDirectory + "/" + SO_PATH + "/";
        return extractBsDiffInternals(context, dir, meta, patchFile, TYPE_Library, isUpgradePatch);
    }

    private static boolean extractBsDiffInternals(Context context, String dir, String meta, File patchFile, int type, boolean isUpgradePatch) {
        //parse
        ArrayList<ShareBsDiffPatchInfo> patchList = new ArrayList<>();

        ShareBsDiffPatchInfo.parseDiffPatchInfo(meta, patchList);

        if (patchList.isEmpty()) {
            TinkerLog.w(TAG, "extract patch list is empty! type:%s:", ShareTinkerInternals.getTypeString(type));
            return true;
        }

        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        //I think it is better to extract the raw files from apk
        Tinker manager = Tinker.with(context);

        try {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            if (applicationInfo == null) {
                // Looks like running on a test Context, so just return without patching.
                TinkerLog.w(TAG, "applicationInfo == null!!!!");
                return false;
            }
            String apkPath = applicationInfo.sourceDir;
            final ZipFile apk = new ZipFile(apkPath);
            final ZipFile patch = new ZipFile(patchFile);

            for (ShareBsDiffPatchInfo info : patchList) {
                final String infoPath = info.path;
                String patchRealPath;
                if (infoPath.equals("")) {
                    patchRealPath = info.name;
                } else {
                    patchRealPath = info.path + "/" + info.name;
                }
                final String fileMd5 = info.md5;
                if (!SharePatchFileUtil.checkIfMd5Valid(fileMd5)) {
                    TinkerLog.w(TAG, "meta file md5 mismatch, type:%s, name: %s, md5: %s", ShareTinkerInternals.getTypeString(type), info.name, info.md5);
                    manager.getPatchReporter().onPatchPackageCheckFail(patchFile, isUpgradePatch, BasePatchInternal.getMetaCorruptedCode(type));
                    return false;
                }
                String middle;

                middle = info.path + "/" + info.name;

                File extractedFile = new File(dir + middle);

                //check file whether already exist
                if (extractedFile.exists()) {
                    if (fileMd5.equals(SharePatchFileUtil.getMD5(extractedFile))) {
                        //it is ok, just continue
                        continue;
                    } else {
                        TinkerLog.w(TAG, "have a mismatch corrupted dex " + extractedFile.getPath());
                        extractedFile.delete();
                    }
                } else {
                    extractedFile.getParentFile().mkdirs();
                }


                String patchFileMd5 = info.patchMd5;
                //it is a new file, just copy
                ZipEntry patchFileEntry = patch.getEntry(patchRealPath);

                if (patchFileEntry == null) {
                    TinkerLog.w(TAG, "patch entry is null. path:" + patchRealPath);
                    manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type, isUpgradePatch);
                    return false;
                }

                if (patchFileMd5.equals("0")) {
                    if (!extract(patch, patchFileEntry, extractedFile, fileMd5, false)) {
                        TinkerLog.w(TAG, "Failed to extract file " + extractedFile.getPath());
                        manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type, isUpgradePatch);
                        return false;
                    }
                } else {
                    //we do not check the intermediate files' md5 to save time, use check whether it is 32 length
                    if (!SharePatchFileUtil.checkIfMd5Valid(patchFileMd5)) {
                        TinkerLog.w(TAG, "meta file md5 mismatch, type:%s, name: %s, md5: %s", ShareTinkerInternals.getTypeString(type), info.name, patchFileMd5);
                        manager.getPatchReporter().onPatchPackageCheckFail(patchFile, isUpgradePatch, BasePatchInternal.getMetaCorruptedCode(type));
                        return false;
                    }

                    ZipEntry rawApkFileEntry = apk.getEntry(patchRealPath);

                    if (rawApkFileEntry == null) {
                        TinkerLog.w(TAG, "apk entry is null. path:" + patchRealPath);
                        manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type, isUpgradePatch);
                        return false;
                    }

                    String rawApkMd5 = info.rawMd5;

                    if (!SharePatchFileUtil.checkIfMd5Valid(rawApkMd5)) {
                        TinkerLog.w(TAG, "meta file md5 mismatch, type:%s, name: %s, md5: %s", ShareTinkerInternals.getTypeString(type), info.name, rawApkMd5);
                        manager.getPatchReporter().onPatchPackageCheckFail(patchFile, isUpgradePatch, BasePatchInternal.getMetaCorruptedCode(type));
                        return false;
                    }

                    BSPatch.patchFast(apk.getInputStream(rawApkFileEntry), patch.getInputStream(patchFileEntry), extractedFile);

                    //go go go bsdiff get the
                    if (!SharePatchFileUtil.verifyFileMd5(extractedFile, fileMd5)) {
                        TinkerLog.w(TAG, "Failed to recover diff file " + extractedFile.getPath());
                        manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type, isUpgradePatch);
                        SharePatchFileUtil.safeDeleteFile(extractedFile);
                        return false;
                    }

                }
            }

        } catch (Exception e) {
//            e.printStackTrace();
            throw new TinkerRuntimeException("patch " + ShareTinkerInternals.getTypeString(type) + " extract failed (" + e.getMessage() + ").", e);
        }
        return true;
    }

}