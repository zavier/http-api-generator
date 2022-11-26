package com.example.httpapigenerator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class PopupDialogAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project currentProject = e.getProject();
        final Navigatable data = e.getData(CommonDataKeys.NAVIGATABLE);

        List<HttpApiData> dataList = new ArrayList<>();
        if (data instanceof PsiDirectory) {
            PsiDirectory directory = (PsiDirectory) data;

            String destPath = directory.getVirtualFile().getPath();

            dataList = operateDirectory((PsiDirectory) data);

            if (dataList.size() > 0) {
                StringBuilder builder = new StringBuilder();
                builder.append("路径").append(",").append("方法").append(",").append("描述").append("\n");
                for (HttpApiData httpApiData : dataList) {
                    builder.append(httpApiData.getPath())
                            .append(",")
                            .append(httpApiData.getMethod())
                            .append(",")
                            .append(httpApiData.getDesc())
                            .append("\n");
                }

                final File file = new File(destPath, "http接口.csv");
                try {
                    final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
                    outputStreamWriter.write(builder.toString());
                    outputStreamWriter.flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        Messages.showMessageDialog(
                currentProject,
                dataList.size() == 0 ? "无数据" : "生成成功",
                "提示",
                Messages.getInformationIcon());
    }


    private List<HttpApiData> operateDirectory(PsiDirectory directory) {
        final PsiElement[] children = directory.getChildren();

        List<HttpApiData> listData = new ArrayList<>();
        for (PsiElement child : children) {
            if (child instanceof PsiDirectory) {
                listData.addAll(operateDirectory((PsiDirectory) child));
            }
            if (child instanceof PsiJavaFile) {
                listData.addAll(operateJavaFile((PsiJavaFile) child));
            }
        }
        return listData;
    }

    private List<HttpApiData> operateJavaFile(PsiJavaFile javaFile) {
        List<HttpApiData> dataList = new ArrayList<>();

        final PsiElement[] children = javaFile.getChildren();
        for (PsiElement child : children) {
            if (child instanceof PsiClass) {
                PsiClass clz = (PsiClass) child;
                // 判断是否是Controller
                if (!clz.hasAnnotation("org.springframework.web.bind.annotation.RestController") && !clz.hasAnnotation("org.springframework.stereotype.Controller")) {
                    continue;
                }

                // 获取类上面的路径注解信息
                String bathPath = "";
                final PsiAnnotation annotation = clz.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
                if (annotation != null) {
                    final PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                    bathPath = value.getText().substring(1, value.getText().length() - 1);
                }

                final PsiMethod[] methods = clz.getMethods();
                for (PsiMethod method : methods) {
                    HttpApiData httpApiData = operateRequestMapping(bathPath, method);
                    if (httpApiData == null) {
                        httpApiData = operateRequestGetMapping(bathPath, method);
                        if (httpApiData == null) {
                            httpApiData = operateRequestPostMapping(bathPath, method);
                        }
                    }
                    if (httpApiData == null) {
                        continue;
                    }

                    dataList.add(httpApiData);
                }
            }
        }
        return dataList;
    }

    @Nullable
    private static HttpApiData operateRequestMapping(String bathPath, PsiMethod method) {
        if (!method.hasAnnotation("org.springframework.web.bind.annotation.RequestMapping")) {
            return null;
        }
        final HttpApiData httpApiData = new HttpApiData();

        final PsiAnnotation requestMappingAnnotation = method.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
        if (requestMappingAnnotation != null) {
            final PsiAnnotationMemberValue value = requestMappingAnnotation.findAttributeValue("value");
            httpApiData.setPath(bathPath + value.getText().substring(1, value.getText().length() - 1));

            final PsiAnnotationMemberValue methodValue = requestMappingAnnotation.findAttributeValue("method");
            httpApiData.setMethod(methodValue.getText().split("\\.")[1]);
        }

        final String doc = getMethodDoc(method);
        httpApiData.setDesc(doc);
        return httpApiData;
    }

    @Nullable
    private static HttpApiData operateRequestGetMapping(String bathPath, PsiMethod method) {
        if (!method.hasAnnotation("org.springframework.web.bind.annotation.GetMapping")) {
            return null;
        }
        final HttpApiData httpApiData = new HttpApiData();

        final PsiAnnotation requestMappingAnnotation = method.getAnnotation("org.springframework.web.bind.annotation.GetMapping");
        if (requestMappingAnnotation != null) {
            final PsiAnnotationMemberValue value = requestMappingAnnotation.findAttributeValue("value");
            httpApiData.setPath(bathPath + value.getText().substring(1, value.getText().length() - 1));
            httpApiData.setMethod("GET");
        }

        final String doc = getMethodDoc(method);
        httpApiData.setDesc(doc);
        return httpApiData;
    }

    @Nullable
    private static HttpApiData operateRequestPostMapping(String bathPath, PsiMethod method) {
        if (!method.hasAnnotation("org.springframework.web.bind.annotation.PostMapping")) {
            return null;
        }
        final HttpApiData httpApiData = new HttpApiData();

        final PsiAnnotation requestMappingAnnotation = method.getAnnotation("org.springframework.web.bind.annotation.PostMapping");
        if (requestMappingAnnotation != null) {
            final PsiAnnotationMemberValue value = requestMappingAnnotation.findAttributeValue("value");
            httpApiData.setPath(bathPath + value.getText().substring(1, value.getText().length() - 1));
            httpApiData.setMethod("POST");
        }

        final String doc = getMethodDoc(method);
        httpApiData.setDesc(doc);
        return httpApiData;
    }

    private static String getMethodDoc(PsiMethod method) {
        final PsiElement[] children1 = method.getChildren();
        StringBuilder builder = new StringBuilder();
        for (PsiElement psiElement : children1) {
            if (psiElement instanceof PsiDocComment) {
                PsiDocComment docComment = (PsiDocComment) psiElement;
                final String text = docComment.getText();
                final String[] split = text.replace("\n", "").split("\\*");
                for (String s : split) {
                    if (s == null || s.trim().length() == 0) {
                        continue;
                    }
                    if (s.trim().equals("/")) {
                        continue;
                    }
                    if (s.contains("@")) {
                        continue;
                    }
                    builder.append(s);
                }
            }
        }
        return builder.toString().trim();
    }
}
