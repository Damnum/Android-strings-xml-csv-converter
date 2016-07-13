package org.pandawarrior

import groovy.xml.MarkupBuilder

/**
 * Created by jt on 5/9/15.
 */
class WriteXml {

    final static String ARRAY_FILE = "arrays"
    final static String PLURALS_FILE = "plurals"
    private static final String REGEX = /,(?=([^\"]*\"[^\"]*\")*[^\"]*$)/
    private static final String LINEBREAK_REGEX = /\n(?=([^\"]*\"[^\"]*\")*[^\"]*$)/

    static boolean parse(String csvPath, String moduleFolder) {
        //load and split the file
        List rows = getRows(csvPath)
        List head = getHead(rows)
        Map mainDict = getMainMap(rows, head)
        Map transDict = getTransMap(rows)
        return writeFile(moduleFolder, mainDict, transDict)
    }

    static boolean parseArray(String csvPath, String moduleFolder, String type) {
        //load and split the file
        List<String[]> rows = getRows(csvPath)
        List<String> head = getHead(rows)
        Map<String, String> transDict = getTransMap(rows, type)
        List<String> nameList = getArrayNameList(transDict)

        Map mainDict
        if (PLURALS_FILE.equals(type)) {
            mainDict = getMainArrayMapForPlurals(rows, head, nameList)
        } else {
            mainDict = getMainArrayMap(rows, head, nameList)
        }
        return writeArrayFile(moduleFolder, mainDict, transDict, type)
    }

    protected static List<String[]> getRows(String csvPath) {
        return getRowsForText(new File(csvPath).getText());

    }

    protected static List<String[]> getRowsForText(String csv) {
        String[] lines = csv.split(LINEBREAK_REGEX)
        List<String[]> rows = lines.collect {
            it.split(REGEX, -1)
        }
        return rows
    }

    protected static List<String> getHead(List<String[]> rows) {
        List<String> head = rows.get(0)
        head = head - "\"name\""
        head = head - "name"
        head = head - "\"translatable\""
        head = head - "translatable"
        head = head - ""
        head = head - "\"quantity\""
        head = head - "quantity"
        //        println head
        return head
    }

    /**
     * Transform rows into a map that specifies whether a string is translatable.
     * In the case of plurals the translatable column is the third row behind the quantity row.
     * @param rows
     * @return map with key: name, value: translatable (true or false)
     */
    protected static Map<String, String> getTransMap(List<String[]> rows, String type = "") {
        Map transMap = [:]
        for (int i = 1; i < rows.size(); i++) {
            transMap[rows[i][0]] = rows[i][PLURALS_FILE.equals(type) ? 2 : 1].replaceAll("\"", "")
        }
        return transMap
    }

    protected static Map getMainMap(List rows, List head) throws ArrayIndexOutOfBoundsException {

        Map mainDict = [:]
//        println rows
        for (int i = 0; i < head.size(); i++) {
            def tempMap = [:]
            for (int j = 1; j < rows.size(); j++) {
                def column = rows.get(j)
                def name = column[0]
                tempMap[name] = column[i + 2]
                System.out.println("column " + i + " row " + j + " tempMap[name] " + tempMap[name])
            }
            mainDict[head[i].replaceAll("\"", "")] = tempMap
        }
        return mainDict
    }

    /**
     * Creates a list of string names (identifiers)
     * @param transMap translations map
     * @return list of names
     */
    protected static List<String> getArrayNameList(Map<String, String> transMap) {
        return transMap.collect { it.key }
    }

    /**
     *
     * @param rows
     * @param head
     * @param nameList
     * @return map with key: column header (language) and value: (map with key: name and value: translation)
     */
    protected
    static Map<String, Map<String, String[]>> getMainArrayMap(List<String[]> rows, List<String> head, List<String> nameList) {
        Map mainArrayMap = [:]

        for (int i = 0; i < head.size(); i++) {
            def tempMap = [:]
            nameList.each { String name ->
                def tempList = []
                for (int j = 1; j < rows.size(); j++) {
                    def row = rows.get(j)
                    def tempName = row[0]
                    if (tempName == name) {
                        tempList.add row[i + 2]
                        tempMap[name] = tempList
                    }

                }
            }
            mainArrayMap[head[i].replaceAll("\"", "")] = tempMap
        }
        return mainArrayMap
    }

    /**
     *
     * @param rows
     * @param head
     * @param nameList
     * @return map with key: column header (language) and value:
     * (map with key: name and value: (map with key: quantity and value: translation)
     */
    protected
    static Map<String, Map<String, Map<String, String>>> getMainArrayMapForPlurals(List<String[]> rows, List<String> head, List<String> nameList) {
        Map mainArrayMap = [:]

        for (int i = 0; i < head.size(); i++) {
            def tempMap = [:]
            nameList.each { String name ->
                def tempQuantityMap = [:]
                for (int j = 1; j < rows.size(); j++) {
                    def row = rows.get(j)
                    def tempName = row[0]
                    if (tempName == name) {
                        tempQuantityMap.put(row[1], row[i + 3])
                        tempMap[name] = tempQuantityMap
                    }

                }
            }
            mainArrayMap[head[i].replaceAll("\"", "")] = tempMap
        }
        return mainArrayMap
    }

    private static boolean writeFile(String destination, Map mainDict, Map transDict) {
        mainDict.each {
            def stringWriter = new StringWriter()
            def xml = new MarkupBuilder(stringWriter)
            //language
            def language = it.value
            def fileName = it.key
            String dir = "${destination}/res/${fileName}/"
            File file = new File(dir, 'strings.xml')
            File folder = new File(dir)

            if (!folder.exists()) {
                folder.mkdirs()
            }
            xml.resources {
                language.each {
                    def name = it.key
                    def value = it.value.replaceAll("\"", "")
                    if (fileName.equals("values") && transDict[name].equals("false") ||
                            fileName.equals("values") && !transDict[name]) {
                        string(name: name.replaceAll("\"", ""), translatable: transDict[name], value)
                    } else if (transDict[name].equals("true") && !value.equals("null") && !value.equals("") ||
                            transDict[name] && !value.equals("null") && !value.equals("")
                    ) {
                        string(name: name.replaceAll("\"", ""), value)
                    }
                }
            }
            def records = stringWriter.toString()
            file.withWriter('utf-8') { writer ->
                writer.write(records)
            }
            return true
        }
    }

    private static boolean writeArrayFile(String destination, Map mainArrayMap, Map transDict, String type) {
        mainArrayMap.each {
            def stringWriter = new StringWriter()
            def xml = new MarkupBuilder(stringWriter)
            def language = it.value
            def fileName = it.key
            def rowName
            String dir = "${destination}/res/${fileName}/"
            File file = new File(dir, "${type}.xml")
            File folder = new File(dir)

            if (!folder.exists()) {
                folder.mkdirs()
            }
            rowName = type == ARRAY_FILE ? "string-array" : type

            xml.resources {
                language.each {
                    def name = it.key
                    def values = it.value.collect()
                    if (fileName.equals("values") && transDict[name].equals("false") ||
                            fileName.equals("values") && !transDict[name]) {
                        "${rowName}"(name: name.replaceAll("\"", ""), translatable: transDict[name]) {
                            values.each {
                                item(it.replaceAll("\"", ""))
                            }
                        }
                    } else if (transDict[name].equals("true") || transDict[name]) {
                        "${rowName}"(name: name.replaceAll("\"", "")) {
                            values.each {
                                if (type == PLURALS_FILE && isNotEmpty(it.value)) {
                                    item(quantity: it.key.replaceAll("\"", ""), it.value.replaceAll("\"", ""))
                                } else if (type == ARRAY_FILE && isNotEmpty(it)) {
                                    item(it.replaceAll("\"", ""))
                                }
                            }
                        }
                    }
                }
            }
            def records = stringWriter.toString()
            file.withWriter('utf-8') { writer ->
                writer.write(records)
            }
            return true
        }
    }

    private static boolean isNotEmpty(String text) {
        return !text.equals("null") && !text.equals(" ") && !text.equals("");
    }
}


