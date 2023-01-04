package com.qncube.docbuider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javadoc.*;

public class Doclet {
    public static final HashMap<String, String> links = new LinkedHashMap<>();

    private static String checkLinker(String name) {
        String ret = DocLinkerUtils.INSTANCE.checkLinker(name);
        System.out.println("checkLinker(String name)" + name + "  " + ret);
        return ret;
    }

    public static void println(ArrayList<String> sources, String outDir) throws NoSuchFieldException, IllegalAccessException {

        ArrayList<String> list = new ArrayList<>();
        list.add("-doclet");
        list.add(Doclet.class.getName());
        list.addAll(sources);
        System.out.println("开始调用doc");
        com.sun.tools.javadoc.Main.execute(list.toArray(new String[list.size()]));

        ClassDoc[] classes = Doclet.root.classes();
        System.out.println("结束调用doc " + classes.length);
        StringBuilder sb = new StringBuilder();

        LinkedList<String> classNameList = new LinkedList<String>();
        for (Map.Entry<String, String> entry : links.entrySet()) {
            classNameList.add(entry.getKey());
        }
        LinkedList<ClassDoc> classDocsList = new LinkedList<ClassDoc>(Arrays.asList(classes));
        classDocsList.sort(Comparator.comparingInt(classDoc -> classNameList.indexOf(classDoc.name()))
        );

        for (ClassDoc classDoc : classDocsList) {
            boolean isBuildDoc = links.containsKey(classDoc.name());
            System.out.println("是否生成文档 " + classDoc.name() + "  " + isBuildDoc);
            if (!isBuildDoc) {
                continue;
            }
            System.out.println(classDoc.name() + "  !");
            DocFormat format = new DocFormat();
            format.name = classDoc.name();
            format.home = false;

            format.describe = new DocFormat.Describe();
            format.describe.content.add(getClassType(classDoc) + " " + classDoc.qualifiedName());
            format.describe.content.add(classDoc.commentText());

            format.reflect = links;

            sb.append("\n//" + replaceBlank2(classDoc.commentText()) + "\n");
            sb.append((classDoc.name()) + "{\n");

            FieldDoc[] fields = classDoc.fields();
            DocFormat.BlockItem filedItem = new DocFormat.BlockItem();

            filedItem.name = "字段";

            for (FieldDoc field : fields) {
                DocFormat.ElementItem i = new DocFormat.ElementItem();
                i.name = field.name();
                i.desc.add(field.commentText());
                i.sign = field.modifiers() + " " + checkLinker(field.type().simpleTypeName()) + " " + field.name();
                filedItem.elements.add(i);
                for (String desc : i.desc) {
                    sb.append("\t").append("//" + replaceBlank2(desc)).append("\n");
                }
                sb.append("\t").append(field.modifiers() + " " + (field.type().simpleTypeName()) + " " + field.name() + "\n");
            }
            if (filedItem.elements.size() > 0) {
                format.blocks.add(filedItem);
            }

            DocFormat.BlockItem methodItem = new DocFormat.BlockItem();
            methodItem.name = "方法";
            if (!classDoc.isEnum()) {
                MethodDoc[] methods = classDoc.methods();
                for (MethodDoc method : methods) {

                    DocFormat.ElementItem elementItem = new DocFormat.ElementItem();
                    elementItem.name = method.name();
                    elementItem.desc.add(method.commentText());
                    elementItem.returns = checkLinker(method.returnType().simpleTypeName());
                    //  elementItem.sign = method.toString();
//                    Class<ProgramElementDocImpl> clz = ProgramElementDocImpl.class;
//                    Field ageField = clz.getDeclaredField("tree");
//                    ageField.setAccessible(true);
//                    JCTree ageValue = (JCTree) ageField.get(method);
//                    String mn = ageValue.toString();
//                    elementItem.sign = mn;

                    Parameter[] parameters = method.parameters();
                    ParamTag[] tags = method.paramTags();

                    if (method.name().equals("auth")) {
                        if (elementItem.note == null) {
                            elementItem.note = new ArrayList<>();
                        }
                        elementItem.note.add("认证成功后才能使用qlive的功能");
                    }

                    int index = 0;

                    sb.append("\n");
                    sb.append("\t").append("//").append(replaceBlank(method.commentText())).append("\n");
                    if (tags.length > 0) {
                        sb.append("\t").append("//");
                    }
                    StringBuilder paramsMap = new StringBuilder();
                    for (ParamTag tag : tags) {
                        DocFormat.ParameterItem parameterItem = new DocFormat.ParameterItem();
                        parameterItem.name = tag.parameterName();
                        parameterItem.desc = tag.parameterComment();
                        parameterItem.type = checkLinker(parameters[index].type().simpleTypeName());
                        elementItem.parameters.add(parameterItem);

                        sb.append("@param-").append(parameterItem.name).append(":").append(parameterItem.desc).append('\t');
                        paramsMap.append(parameters[index].type().simpleTypeName()).append(" ").append(parameterItem.name);
                        index++;
                        if (index != tags.length) {
                            paramsMap.append(",");
                        }
                    }
                    String mn = method.modifiers() + " " + method.returnType() + " " + method.name() + "(" + paramsMap.toString() + ")";
                    elementItem.sign = mn;
                    if (tags.length > 0) {
                        sb.append("\n");
                    }
                    //   sb.append("\t").append((method.modifiers() + " " + method.returnType().simpleTypeName() + " " + method.name() + " " + method.flatSignature())).append(replaceBlank(method.commentText())).append(replaceBlank(method.commentText())).append("\n");
                    sb.append("\t").append(mn).append("\n");

                    methodItem.elements.add(elementItem);
                }

                if (methods.length > 0) {
                    format.blocks.add(methodItem);
                }
            }
            if (format.blocks.size() > 0) {
                System.out.println(links.get(classDoc.name()));
                System.out.println(format.toJson());
            }
            sb.append("}\n");
        }
        System.out.println("api概览");
        DocFormat format = new DocFormat();
        format.name = "api概览";
        format.home = true;
        format.reflect = links;

        for (ClassDoc classDoc : classDocsList) {
            if (links.get(classDoc.name()) == null) {
                continue;
            }
            DocFormat.BlockItem classItem = new DocFormat.BlockItem();
            classItem.name = "";
            classItem.desc.add(checkLinker(classDoc.name()));
            classItem.desc.add(classDoc.commentText());
            format.blocks.add(classItem);
        }

//        writeToFile(format.toJson(), outDir + "/doc.txt");
//        writeToFile(sb.toString(), outDir + "/md.txt");

        System.out.println(format.toJson());

        System.out.println(sb.toString());
    }

    private static void writeToFile(String str, String outPath) {

        try {
            File file = new File(outPath);
            if (file.exists()) {
                file.delete();
            }
            FileWriter fileWritter = new FileWriter(file.getName(), true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);

            bufferWritter.write(str + "\n");

            bufferWritter.flush();
            bufferWritter.close();
            fileWritter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getClassType(ClassDoc classDoc) {
        if (classDoc.isEnum()) {
            return "enum";
        }
        if (classDoc.isInterface()) {
            return "interface";
        }
        if (classDoc.isClass()) {
            return "class";
        }
        return "";
    }

    /**
     * 文档根节点
     */
    private static RootDoc root;

    /**
     * javadoc调用入口
     *
     * @param root
     * @return
     */
    public static boolean start(RootDoc root) {
        Doclet.root = root;
        return true;
    }

    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\\t|\\r|\\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    public static String replaceBlank2(String str) {
        String dest = str.replaceAll("\n", " ");
        return dest;
    }
}
