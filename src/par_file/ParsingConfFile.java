package par_file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class ParsingConfFile implements ParsingConfFileInterface {
	private Map<String, String> confs;
	private BufferedReader buff;
	private StringTokenizer toks;
	private String line;
	public ParsingConfFile(String file_name, String[] all_known_confs) throws IOException, EmptyFileException, IllegalFileFormatException {
		this.confs=new HashMap<String, String>();
		for(String c: all_known_confs)
			this.confs.put(c, null);
		this.buff=new BufferedReader(new FileReader(file_name));
		this.load_confs();
	}	
	
	private void load_confs() throws IOException, EmptyFileException, IllegalFileFormatException {
		line=buff.readLine();
		if(line == null)
			throw new EmptyFileException("File empty");
		while(line != null) {
			if(!line.startsWith("#")) {
				toks = new StringTokenizer(line, "=");
				while(toks.hasMoreTokens()) {
					String val=null;
					String conf=null;
					if(!confs.containsKey((conf=toks.nextToken())))
						throw new IllegalFileFormatException();
					if(toks.hasMoreTokens()) {
						val=toks.nextToken();
						val = val.trim();
						toks=new StringTokenizer(val);
						val=toks.nextToken();
						confs.put(conf, val);
						break;
					}
				}
			}
			line=buff.readLine();
		}
		
		
	}
	public String getConf(String conf_name) {
		return this.confs.get(conf_name);
	}
}

