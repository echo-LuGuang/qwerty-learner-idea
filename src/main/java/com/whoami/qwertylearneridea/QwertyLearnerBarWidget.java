package com.whoami.qwertylearneridea;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings;
import com.intellij.util.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class QwertyLearnerBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {
    private final SelectionListener selectionListener;

    String text = "QwertyLearner";

    OkHttpClient client = new OkHttpClient();

    public QwertyLearnerBarWidget(Project project) {
        selectionListener = new SelectionListener() {
            @Override
            public void selectionChanged(@NotNull SelectionEvent e) {
                TextRange range = e.getNewRange();
                Editor editor = e.getEditor();
                Document document = editor.getDocument();
                String selectedText = document.getText(range);
                if (!selectedText.isEmpty() && (isEnglishStr(selectedText) || isChineseStr(selectedText))) {
                    String url = "https://dict.youdao.com/suggest?num=1&ver=3.0&doctype=json&cache=false&le=zh&q=" + selectedText;
                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        String resText = response.body().string();
                        JSONObject res = JSON.parseObject(resText);
                        if (res.getJSONObject("result").getInteger("code") == 200) {
                            JSONObject data = res.getJSONObject("data").getJSONArray("entries").getJSONObject(0);
                            text = data.getString("entry") + "：" + data.getString("explain");
                        }

                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    text = "";
                }

                if (project != null) {
                    StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                    statusBar.updateWidget(ID());
                }
            }
        };
    }

    @Override
    public @NotNull String ID() {
        return "QwertyLearnerBarWidget";
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        // 监听选中的文字
        EditorFactory.getInstance().getEventMulticaster().addSelectionListener(selectionListener, this);
    }

    @Override
    public void dispose() {
        //停止监听
        EditorFactory.getInstance().getEventMulticaster().removeSelectionListener(selectionListener);
    }

    @Override
    public @NotNull String getText() {
        return text;
    }


    @Override
    public float getAlignment() {
        return Component.CENTER_ALIGNMENT;
    }

    @Override
    public @Nullable String getTooltipText() {
        return "Qwerty-learner-idea";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    public static boolean isEnglishStr(String charaString) {
        return charaString.matches("^[a-zA-Z]*");
    }


    public static boolean isChineseStr(String str) {
        String regEx = "[\\u4e00-\\u9fa5]+";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }
}
