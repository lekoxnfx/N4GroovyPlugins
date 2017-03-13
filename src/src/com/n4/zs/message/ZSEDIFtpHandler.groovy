package src.com.n4.zs.message

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

class ZSEDIFtpHandler {
	FTPClient ftp = new FTPClient();
	String ip = "10.18.0.4";
	int port = 21;
	String username = "ZSDND";
	String passwd = "ZSDND";
	String uploadtemppath = "/in_temp";
	String uploadpath = "/in";
	
	/**
	 * Description: 向FTP服务器上传文件
	 * @param url FTP服务器hostname
	 * @param port FTP服务器端口
	 * @param username FTP登录账号
	 * @param password FTP登录密码
	 * @param path FTP服务器保存目录
	 * @param filename 上传到FTP服务器上的文件名
	 * @param input 输入流
	 * @return 成功返回true，否则返回false
	 */
	public boolean uploadFile(File f) {
		boolean success = false;

		try {
			int reply;
			
			ftp.connect(ip, port);//连接FTP服务器
			//如果采用默认端口，可以使用ftp.connect(url)的方式直接连接FTP服务器
			ftp.login(username, passwd);//登录
			reply = ftp.getReplyCode();
			System.out.println(reply);
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return success;
			}
			ftp.setControlEncoding("UTF-8");
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE, FTPClient.BINARY_FILE_TYPE);
			ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
			ftp.enterLocalPassiveMode();
			//先传输到临时目录
			String filename = f.getName();
			filename=new String(filename.getBytes(),"iso-8859-1");
			InputStream input =new FileInputStream(f);
			ftp.changeWorkingDirectory(uploadtemppath);
			ftp.storeFile(filename, input);
			input.close();
			//移动到正式目录
			success = ftp.rename(filename, uploadpath+"/"+filename);
			reply = ftp.getReplyCode();
			ftp.logout();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (Exception ioe) {
				}
			}
		}
		return success;
	}
	public List<File> downloadFiles(String remoteDir,String remoteTempDir,String localDir){
		ArrayList<File> file_list = new ArrayList<File>();
		try {
			int reply;
			System.out.println(username+" "+passwd);
			
			ftp.connect(ip, port);//连接FTP服务器
			//如果采用默认端口，可以使用ftp.connect(url)的方式直接连接FTP服务器
			ftp.login(username, passwd);//登录
			reply = ftp.getReplyCode();
			String encoding = "GBK";
			System.out.println(encoding);
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return null;
			}
//			FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_NT);
//			conf.setServerLanguageCode("zh");
			ftp.setControlEncoding(encoding);
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE, FTPClient.BINARY_FILE_TYPE);
			ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
//
			//打开下载目录
			List<String> file_names = new ArrayList<String>();
			ftp.changeWorkingDirectory(remoteDir);
			ftp.enterLocalPassiveMode();
			FTPFile[] remote_files = ftp.listFiles();
			
			if(remote_files != null){
				//先移动到临时目录
				for(FTPFile ftp_file : remote_files){
					String fname = new String(ftp_file.getName().getBytes(encoding),"iso-8859-1");
					boolean success = ftp.rename(fname, remoteTempDir+"/"+ fname);
					System.out.println(success);
					
					System.out.println(ftp_file.getName());
					file_names.add(ftp_file.getName()); //此时file_names存储的是文件名
				}
				
				//将file_names中的文件全部取回本地
					//先读取临时目录中的所有文件
				ftp.changeWorkingDirectory(remoteTempDir);
				remote_files = ftp.listFiles(remoteTempDir);
				for (FTPFile ff : remote_files) {
					
					 if (!ff.isDirectory()) {
						 if(file_names.contains(ff.getName())){
							 File dir = new File(localDir);
							 if(!dir.exists()){
								 dir.mkdirs();
							 }
							 String localPath = dir.getPath() + "/" + ff.getName();
							 System.out.println(localPath);
							 File localFile = new File(localPath);
							 FileOutputStream fos = new FileOutputStream(localFile);
	//						 System.out.println(ff.getName());//
							 ftp.retrieveFile(new String(ff.getName().getBytes(encoding),"ISO-8859-1"), fos);
							 fos.flush();
							 fos.close();
							 file_list.add(localFile);
							 ftp.deleteFile(remoteTempDir+"/"+new String(ff.getName().getBytes(),"iso-8859-1"));
							 reply = ftp.getReplyCode();
							 System.out.println(reply);
							}
						
					 }
				}
			}
			ftp.logout();
			return file_list;
		} catch (Exception e) {
			e.printStackTrace();
			return file_list;
		} finally {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (Exception ioe) {
					return file_list;
				}
			}
		}
	}
}
