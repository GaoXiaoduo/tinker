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

package tinker.sample.android.reporter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.widget.Toast;

import com.tencent.tinker.lib.reporter.DefaultLoadReporter;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.shareutil.ShareConstants;

import java.io.File;

import tinker.sample.android.util.UpgradePatchRetry;
import tinker.sample.android.util.Utils;

/**
 * optional, you can just use DefaultLoadReproter
 * Created by shwenzhang on 16/4/13.
 */
public class SampleLoadReporter extends DefaultLoadReporter {
    private Handler handler = new Handler();

    public SampleLoadReporter(Context context) {
        super(context);
    }

    @Override
    public void onLoadPatchListenerReceiveFail(final File patchFile, int errorCode, final boolean isUpgrade) {
        super.onLoadPatchListenerReceiveFail(patchFile, errorCode, isUpgrade);
        switch (errorCode) {
            case ShareConstants.ERROR_PATCH_NOTEXIST:
                Toast.makeText(context, "patch file is not exist", Toast.LENGTH_LONG).show();
                break;
            case ShareConstants.ERROR_PATCH_RUNNING:
                // try later
                // only retry for upgrade patch
                if (isUpgrade) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            TinkerInstaller.onReceiveUpgradePatch(context, patchFile.getAbsolutePath());
                        }
                    }, 60 * 1000);
                }
                break;
            case Utils.ERROR_PATCH_ROM_SPACE:
                Toast.makeText(context, "rom space is not enough", Toast.LENGTH_LONG).show();
                break;
        }

    }

    @Override
    public void onLoadResult(File patchDirectory, int loadCode, long cost) {
        super.onLoadResult(patchDirectory, loadCode, cost);
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override public boolean queueIdle() {
                UpgradePatchRetry.getInstance(context).onPatchRetryLoad();
                return false;
            }
        });
    }
}