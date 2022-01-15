package com.ichi2.utils;

import java.util.HashMap;
import java.util.Map;

public class Bcp47Parser {
    public static Map<String, String> BCP47 = new HashMap<>();
    static {
        BCP47.put("af-ZA", "南非荷兰语（南非）");
        BCP47.put("am-ET", "阿姆哈拉语（埃塞俄比亚）");
        BCP47.put("ar-BH", "阿拉伯语(巴林)，现代标准");
        BCP47.put("ar-EG", "阿拉伯语（埃及）");
        BCP47.put("ar-IQ", "阿拉伯语（伊拉克）");
        BCP47.put("ar-IL", "阿拉伯语（以色列）");
        BCP47.put("ar-JO", "阿拉伯语（约旦）");
        BCP47.put("ar-KW", "阿拉伯语（科威特）");
        BCP47.put("ar-LB", "阿拉伯语（黎巴嫩）");
        BCP47.put("ar-LY", "阿拉伯语（利比亚）");
        BCP47.put("ar-MA", "阿拉伯语（摩洛哥）");
        BCP47.put("ar-OM", "阿拉伯语（阿曼）");
        BCP47.put("ar-QA", "阿拉伯语（卡塔尔）");
        BCP47.put("ar-SA", "阿拉伯语(沙特阿拉伯)");
        BCP47.put("ar-PS", "阿拉伯语（巴勒斯坦民族权利机构）	");
        BCP47.put("ar-SY", "阿拉伯语（叙利亚）");
        BCP47.put("ar-TN", "阿拉伯语（突尼斯）");
        BCP47.put("ar-AE", "阿拉伯语（阿拉伯联合酋长国）");
        BCP47.put("ar-YE", "阿拉伯语（也门）");
        BCP47.put("bg-BG", "保加利亚语(保加利亚)");
        BCP47.put("ca-ES", "加泰罗尼亚语(西班牙)");

        BCP47.put("zh-HK", "中文（粤语，繁体)");
        BCP47.put("zh-CN", "中文（普通话，简体)");
        BCP47.put("zh-TW", "中文(台湾普通话)");

        BCP47.put("hr-HR", "克罗地亚语（克罗地亚)");

        BCP47.put("cs-CZ", "捷克语（捷克)");

        BCP47.put("da-DK", "丹麦语（丹麦)");

        BCP47.put("nl-NL", "荷兰语（荷兰)");


        BCP47.put("en-AU", "英语（澳大利亚)");


        BCP47.put("en-CA", "英语（加拿大)");


        BCP47.put("en-GH", "英语（加纳）");

        BCP47.put("en-HK", "英语（香港）");

        BCP47.put("en-IN", "英语（印度）");


        BCP47.put("en-IE", "英语（爱尔兰)");

        BCP47.put("en-KE", "英语（肯尼亚)");

        BCP47.put("en-NZ", "英语（新西兰)");


        BCP47.put("en-NG", "英语（尼日利亚)");

        BCP47.put("en-PH", "英语（菲律宾)");

        BCP47.put("en-SG", "英语（新加坡)");

        BCP47.put("en-ZA", "英语（南非）");

        BCP47.put("en-TZ", "英语（坦桑尼亚)");

        BCP47.put("en-GB", "英语（英国）");


        BCP47.put("en-US", "英语（美国）");


        BCP47.put("et-EE", "爱沙尼亚语（爱沙尼亚)");

        BCP47.put("fil-P", "菲律宾语（菲律宾）");

        BCP47.put("fi-FI", "芬兰语（芬兰)");

        BCP47.put("fr-CA", "法语（加拿大)");


        BCP47.put("fr-FR", "法语（法国）");


        BCP47.put("fr-CH", "法语（瑞士）");

        BCP47.put("de-AT", "德语（奥地利)");

        BCP47.put("de-CH", "德语（瑞士）");

        BCP47.put("de-DE", "德语（德国）");


        BCP47.put("el-GR", "希腊语(希腊)");
        BCP47.put("gu-IN", "古吉拉特语(印度)");
        BCP47.put("he-IL", "希伯来语（以色列）");
        BCP47.put("hi-IN", "印地语（印度)");

        BCP47.put("hu-HU", "匈牙利语(匈牙利)");

        BCP47.put("id-ID", "印度尼西亚语(印度尼西亚)");

        BCP47.put("ga-IE", "爱尔兰语（爱尔兰）");

        BCP47.put("it-IT", "意大利语（意大利）");


        BCP47.put("ja-JP", "日语（日本）");
        BCP47.put("kn-IN", "卡纳达语(印度)");
        BCP47.put("ko-KR", "韩语(韩国)");

        BCP47.put("lv-LV", "拉脱维亚语(拉脱维亚)");

        BCP47.put("lt-LT", "立陶宛语(立陶宛)");

        BCP47.put("ms-MY", "马来语（马来西亚）");
        BCP47.put("mt-MT", "马耳他语（马耳他）");
        BCP47.put("mr-IN", "马拉地语(印度)");
        BCP47.put("nb-NO", "挪威语（博克马尔语，挪威)");
        BCP47.put("fa-IR", "波斯语（伊朗)");
        BCP47.put("pl-PL", "波兰语（波兰)");

        BCP47.put("pt-BR", "葡萄牙语(巴西)");


        BCP47.put("pt-PT", "葡萄牙语(葡萄牙)");

        BCP47.put("ro-RO", "罗马尼亚语(罗马尼亚)");

        BCP47.put("ru-RU", "俄语（俄罗斯)");

        BCP47.put("sk-SK", "斯洛伐克语(斯洛伐克)");

        BCP47.put("sl-SI", "斯洛文尼亚语(斯洛文尼亚)");

        BCP47.put("es-AR", "西班牙语（阿根廷）");

        BCP47.put("es-BO", "西班牙语（玻利维亚）");

        BCP47.put("es-CL", "西班牙语（智利)");

        BCP47.put("es-CO", "西班牙语（哥伦比亚）");

        BCP47.put("es-CR", "西班牙语（哥斯达黎加)");

        BCP47.put("es-CU", "西班牙语（古巴)");

        BCP47.put("es-DO", "西班牙语（多米尼加共和国）");

        BCP47.put("es-EC", "西班牙语（厄瓜多尔）");

        BCP47.put("es-SV", "西班牙语（萨尔瓦多）");

        BCP47.put("es-GQ", "西班牙语（赤道几内亚)");
        BCP47.put("es-GT", "西班牙语（危地马拉）");

        BCP47.put("es-HN", "西班牙语（洪都拉斯）");

        BCP47.put("es-MX", "西班牙语(墨西哥)");


        BCP47.put("es-NI", "西班牙（尼加拉瓜）");

        BCP47.put("es-PA", "西班牙语（巴拿马）");

        BCP47.put("es-PY", "西班牙语（巴拉圭）");

        BCP47.put("es-PE", "西班牙语（秘鲁)");

        BCP47.put("es-PR", "西班牙语（波多黎各）");

        BCP47.put("es-ES", "西班牙语(西班牙)");


        BCP47.put("es-UY", "西班牙语（乌拉圭）");

        BCP47.put("es-US", "西班牙语（美国)");

        BCP47.put("es-VE", "西班牙语（委内瑞拉）");

        BCP47.put("sw-KE", "斯瓦希里语（肯尼亚）");
        ;
        BCP47.put("sv-SE", "瑞典语（瑞典)");

        BCP47.put("ta-IN", "泰米尔语（印度)");
        BCP47.put("te-IN", "泰卢固语（印度)");
        BCP47.put("th-TH", "泰语（泰国）");
        BCP47.put("tr-TR", "土耳其语（土耳其）");
        BCP47.put("vi-VN", "越南语(越南)");


    }


}
