package com.zhongba01.service.impl;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.zhongba01.domain.gou.Account;
import com.zhongba01.mapper.gou.AccountMapper;
import com.zhongba01.service.AccountService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * 中巴价值投资研习社
 *
 * @ author: tangjianhua
 * @ date: 2017/12/13.
 */
@Service
public class AccountServiceImpl implements AccountService {
    private final static String ROOT_PATH = "http://weixin.sogou.com/weixin";

    @Autowired
    AccountMapper accountMapper;

    @Override
    public void dumpAccounts(String keywords) {
        final WebClient webClient = new WebClient(BrowserVersion.CHROME);

        try {
            final String query = URLEncoder.encode(keywords, "utf-8");
            String url = ROOT_PATH + "?query=" + query + "&_sug_type_=&s_from=input&_sug_=y&type=1&ie=utf8";
            boolean hasNext = true;

            //10秒
            long duration = 1000 * 10;
            while (hasNext) {
                final HtmlPage htmlPage = webClient.getPage(url);
                Document document = Jsoup.parse(htmlPage.asXml());
                Elements elements = document.select(".news-list2 > li");

                parseAccounts(elements);

                Element nextPage = document.selectFirst("#pagebar_container #sogou_next");
                if (null == nextPage) {
                    hasNext = false;
                } else {
                    url = ROOT_PATH + nextPage.attr("href");
                    System.out.println(url);

                    Thread.sleep(duration);
                }
            }
            System.out.println("done");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        webClient.close();

        List<Account> accountList = accountMapper.findAll();
        accountList.forEach(System.out::println);

        int count = accountMapper.count();
        System.out.println("共有微信公众号：" + count);
    }

    /**
     * 解析微信公众号信息
     *
     * @param elements box集合
     */
    private void parseAccounts(Elements elements) {
        for (Element el : elements) {
            String avatar = el.selectFirst(".img-box img").attr("src");
            String nickname = el.selectFirst(".txt-box .tit").text().replace(" ", "");
            String wxAccount = el.selectFirst(".txt-box .info label[name='em_weixinhao']").text();

            Account account = accountMapper.findByWxAccount(wxAccount);
            if (null != account) {
                break;
            }

            String description = null, vname = null;
            LocalDateTime lastPublish = null;
            Elements dlList = el.select("dl");
            for (Element dl : dlList) {
                if ("功能介绍：".equalsIgnoreCase(dl.selectFirst("dt").text())) {
                    description = dl.selectFirst("dd").text();
                }
                if ("微信认证：".equalsIgnoreCase(dl.selectFirst("dt").text())) {
                    vname = dl.selectFirst("dd").text();
                }
                if ("最近文章：".equalsIgnoreCase(dl.selectFirst("dt").text())) {
                    lastPublish = parseTime(dl.selectFirst("dd span script").html());
                }
            }

            account = new Account();
            account.setNickname(nickname);
            account.setAccount(wxAccount);
            account.setDescription(description);
            account.setVname(vname);
            account.setAvatar(avatar);
            account.setActive(1);
            account.setLastPublish(lastPublish);
            account.setCreatedAt(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            accountMapper.insert(account);
        }
    }

    /**
     * 解析时间
     *
     * @param string，eg: "document.write(timeConvert('1474348154'))"
     * @return datetime
     */
    private LocalDateTime parseTime(String string) {
        final int expectedLength = 3;
        String[] array = string.split("'");
        if (array.length == expectedLength) {
            return LocalDateTime.ofEpochSecond(Long.valueOf(array[1]), 0, ZoneOffset.ofHours(8));
        }
        return null;
    }
}
