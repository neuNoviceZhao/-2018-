package TianChi;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

public class FileIO {
    public static List<String[]> readCsvFile(String filePath){
        
            ArrayList<String[]> csvList = new ArrayList<String[]>(); 
         try {   
        	CsvReader reader = new CsvReader(filePath,',',Charset.forName("GBK"));
            while(reader.readRecord()){
                csvList.add(reader.getValues()); //按行读取，并把每一行的数据添加到list集合
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
         return csvList;
    }
    
    public static void writeFileToCsv(String[] str, String file) {
		File f = new File(file);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
			CsvWriter cwriter = new CsvWriter(writer,',');
			cwriter.writeRecord(str,false);
			cwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

}