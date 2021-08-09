package com.ichi2.bd;

import android.os.Handler;
import android.util.Log;

import com.baidu.tts.client.SpeechError;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import timber.log.Timber;

/**
 * 保存回调音频流到文件。您也可以直接处理音频流
 * FileSaveListener 在UiMessageListener的基础上，使用 onSynthesizeDataArrived回调，获取音频流
 * Created by fujiayi on 2017/9/15.
 */

public class FileSaveListener extends UiMessageListener {

    /**
     * 保存的文件名 baseName + utteranceId， 通常是 output-0.pcm
     */
    private String baseName = "";

    /**
     * 保存文件的目录
     */
    private String destDir;

    /**
     * 文件
     */
    private File ttsFile;


    /**
     * ttsFile 文件流
     */
    private FileOutputStream ttsFileOutputStream;

    /**
     * ttsFile 文件buffer流
     */
    private BufferedOutputStream ttsFileBufferedOutputStream;

    private static final String TAG = "FileSaveListener";


    public FileSaveListener(Handler mainHandler, String destDir) {
        super(mainHandler);
        this.destDir = destDir;
    }


    private String preErrorFile = "";


    @Override
    public void onSynthesizeStart(String utteranceId) {
        String[] temp = utteranceId.split("-");
        int sectionIndex = Integer.parseInt(temp[0]);
        int endSectionIndex = Integer.parseInt(temp[1]);
        String id = temp[2];
        String cardSide = temp[3];
        String filename = id +"-"+cardSide+ ".pcm";
        // 保存的语音文件是 16K采样率 16bits编码 单声道 pcm文件。
        ttsFile = new File(destDir, filename);
        Timber.i("try to write audio file to %s", ttsFile.getAbsolutePath());
        try {
            if (sectionIndex == 0) {
                if (!ttsFile.exists()) {
                    ttsFile.delete();
                }
                File pre = new File(preErrorFile);
                if (pre.exists()) {
                    pre.delete();
                }
                preErrorFile = ttsFile.getAbsolutePath();
                close("");

                ttsFile.createNewFile();
                // 创建FileOutputStream对象
                FileOutputStream ttsFileOutputStream = new FileOutputStream(ttsFile);
                // 创建BufferedOutputStream对象
                ttsFileBufferedOutputStream = new BufferedOutputStream(ttsFileOutputStream);
            } else {
                //do nothing
            }


        } catch (IOException e) {
            // 请自行做错误处理
            e.printStackTrace();
            sendMessage("创建文件失败:" + destDir + "/" + filename);
            throw new RuntimeException(e);
        }
        sendMessage("创建文件成功:" + destDir + "/" + filename);

    }


    /**
     * 语音流 16K采样率 16bits编码 单声道 。
     *
     * @param utteranceId
     * @param data        二进制语音 ，注意可能有空data的情况，可以忽略
     * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法保证和合成到第几个字对应。
     * @param engineType  1:音频数据由离线引擎合成； 0：音频数据由在线引擎（百度服务器）合成。
     */
    @Override
    public void onSynthesizeDataArrived(String utteranceId, byte[] data, int progress, int engineType) {
        super.onSynthesizeDataArrived(utteranceId, data, progress, engineType);
        Timber.i("合成进度回调, progress：" + progress + ";序列号:" + utteranceId);
        try {
            ttsFileBufferedOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSynthesizeFinish(String utteranceId) {
        super.onSynthesizeFinish(utteranceId);
        String[] temp = utteranceId.split("-");

        String id = temp[2];
        String cardSide = temp[3];
        String filename = id +"-"+cardSide+ ".pcm";
        // 保存的语音文件是 16K采样率 16bits编码 单声道 pcm文件。
        ttsFile = new File(destDir, filename);
        if (temp[0].equals(temp[1])) {
            close(ttsFile.getAbsolutePath());
        }
    }


    /**
     * 当合成或者播放过程中出错时回调此接口
     *
     * @param utteranceId
     * @param speechError 包含错误码和错误信息
     */
    @Override
    public void onError(String utteranceId, SpeechError speechError) {
        String[] temp = utteranceId.split("-");
        String id = temp[2];
        String cardSide = temp[3];
        String filename = id +"-"+cardSide+ ".pcm";
        // 保存的语音文件是 16K采样率 16bits编码 单声道 pcm文件。
        ttsFile = new File(destDir, filename);
        ttsFile.delete();
        close("");
        super.onError(utteranceId, speechError);
    }


    /**
     * 关闭流，注意可能stop导致该方法没有被调用
     */
    private void close(String finalFile) {
        if (ttsFileBufferedOutputStream != null) {
            try {
                ttsFileBufferedOutputStream.flush();
                ttsFileBufferedOutputStream.close();
                ttsFileBufferedOutputStream = null;
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        if (ttsFileOutputStream != null) {
            try {
                ttsFileOutputStream.close();
                ttsFileOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!finalFile.isEmpty()) {
            PCM2Wav.outWav(destDir, new File(finalFile));
            new File(finalFile).delete();
        }
        sendMessage("关闭文件成功");
    }
}
