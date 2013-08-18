/*
 * 本程序由东方巽雷开发
 */
#include <stdio.h>
/*void usage()
{
	fprintf(stderr,"exbootimg,a tool written by dongfangxunlei,can extract android boot.img and recovery.img.\n");
	fprintf(stderr,"usage:\n");
	fprintf(stderr,"\texbootimg boot.img \n");
	return;
}
*/

int exbootimg_main(int argc,char **argv)
{
	static unsigned long kernel_size,kernel_base,ramdisk_size,ramdisk_base,second_stage_size,second_stage_base;
	int page_size = 0;
	char board[4*4+1];
	char cmdline[4*128+1];
	FILE *infile,*kernel,*ramdisk,*second_stage;
	long tmpnum = 1;
	long i;
	if(argc != 2){
		//usage();
		return -1;
	}
	infile = fopen(argv[1],"rb");
	if(infile == NULL){
		return -1;
	}
	fseek(infile,8,SEEK_SET);
	for(i=0;i<4;++i){
		kernel_size += fgetc(infile) * tmpnum;
		tmpnum *= 256;
	}
	tmpnum = 1;
	for(i=0;i<4;++i){
		kernel_base += fgetc(infile) * tmpnum;
		tmpnum *= 256;
	}
	kernel_base -= 0x8000;
	tmpnum = 1;
	for(i=0;i<4;i++){
		ramdisk_size += fgetc(infile) * tmpnum;
		tmpnum *= 256;
	}
	tmpnum = 1;
	for(i=0;i<4;++i){
		ramdisk_base += fgetc(infile) *tmpnum;
		tmpnum *= 256;
	}
	ramdisk_base -= 0x1000000;
	tmpnum =1;
	for(i=0;i<4;++i){
		second_stage_size += fgetc(infile) *tmpnum;
		tmpnum *= 256;
	}
	for(i=0;i<4;++i){
		second_stage_base += fgetc(infile) * tmpnum;
		tmpnum *= 256;
	}
	second_stage_base -= 0xf00000;
	tmpnum =1;
	fseek(infile,4,SEEK_CUR);
	for(i=0;i<4;i++){
		page_size += fgetc(infile) * tmpnum;
		tmpnum *= 256;
	}
	fseek(infile,8,SEEK_CUR);
	fgets(board,(16+1)*sizeof(char),infile);
	fgets(cmdline,(128*4+1)*sizeof(char),infile);
	//cmdline[4*128] = '\0';

	fseek(infile,page_size,SEEK_SET);
	kernel = fopen("kernel","wb");
	if(kernel == NULL)
		return -1;
	for(i=0;i<kernel_size;++i)
		fputc(fgetc(infile),kernel);
	while(fgetc(infile)==0);
	fseek(infile,-1,SEEK_CUR);
	ramdisk = fopen("ramdisk.cpio.gz","w");
	if(ramdisk == NULL)
		return -1;
	for(i=0;i<ramdisk_size;++i)
		fputc(fgetc(infile),ramdisk);
	if(second_stage_size != 0x0){
		while(fgetc(infile)==0);
		fseek(infile,-1,SEEK_CUR);
		second_stage = fopen("second","w");
		for(i=0;i<second_stage_size;++i)
			fputc(fgetc(infile),second_stage);
		fclose(second_stage);
	}
	fclose(infile);
	fclose(kernel);
	fclose(ramdisk);

	if(second_stage_size ==0)
		if(cmdline[0] == 0)
			if(board[0] == 0)
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --base 0x%lx --pagesize %d -o new.img",kernel_base,page_size);
			else
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --board \"%s\" --base 0x%lx --pagesize %d -o new.img",board,kernel_base,page_size);
		else
			if(board[0] == 0)
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --cmdline \"%s\" --base 0x%lx --pagesize %d -o new.img",cmdline,kernel_base,page_size);
			else
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --cmdline \"%s\" --board \"%s\" --base 0x%lx --pagesize %d -o new.img",cmdline,board,kernel_base,page_size);
	else
		if(cmdline[0] == 0)
			if(board[0] == 0)
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --second second --base 0x%lx --pagesize %d -o new.img",kernel_base,page_size);
			else
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --second second --board \"%s\" --base 0x%lx --pagesize %d -o new.img",board,kernel_base,page_size);
		else
			if(board[0] == 0)
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --second second --cmdline \"%s\" --base 0x%lx --pagesize %d -o new.img",cmdline,kernel_base,page_size);
			else
				printf("/data/data/per.pqy.apktool/lix/mkbootimg --kernel kernel --ramdisk ramdisk.cpio.gz  --second second --cmdline \"%s\" --board \"%s\" --base 0x%lx --pagesize %d -o new.img",cmdline,board,kernel_base,page_size);
	return 0;
}


			



