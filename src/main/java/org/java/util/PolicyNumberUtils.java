package org.java.util;

import com.eaio.uuid.UUID;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Enumeration;

/**
 * 保单号工具类
 */
public class PolicyNumberUtils {


    /**
     * 生成一个保单号
     * @return
     */
    public static String makePolicyNumber(){
        String uuid = new UUID().toString();
        Date date = new Date();
        int year = date.getYear()+1900;
        int month = date.getMonth()+1;
        int date1 = date.getDate();
        int hours = date.getHours();
        int minutes = date.getMinutes();
        int seconds = date.getSeconds();
        String hashCode = uuid.hashCode()+"";
        String policyNumber = year+""+month+date1+hours+minutes+seconds+hashCode.substring(1);
        return policyNumber;
    }
}
