/*
 * 本程序由东方巽雷开发
 */

#include <stdio.h>
int revise_main(int argc,char **argv)
{
  long fsize;
  FILE *f1,*f2;
  char header[512];
  if(argc != 3)
    return -1;
  f1=fopen(argv[1],"r");
  f2=fopen(argv[2],"r");
  if(f1 ==NULL || f2 == NULL)
    return -1;
  fseek(f1,0L,SEEK_END);
  fsize = ftell(f1);
  fseek(f2,0L,SEEK_SET);
  fread(header,sizeof(char),512,f2);
  fclose(f2);
  header[4] = fsize % 256;
  header[5] = fsize % (256L * 256L) / 256;
  header[6] = fsize % (256L *256L *256L) / (256L *256L);
 // header[7] = fsize % (256L *256L *256L *256L) / (256L*256L*256L);
  header[7] = 0x0;
  f2 = fopen(argv[2],"w");
  if(f2 == NULL)
    return -1;
  fwrite(header,sizeof(char),512,f2);
  fseek(f1,0L,SEEK_SET);
  while(fsize>0){
    fputc(fgetc(f1),f2);
    fsize--;
  }
fclose(f1);
fclose(f2);
return 0;
}