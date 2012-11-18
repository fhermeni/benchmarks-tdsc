#!/usr/bin/Rscript
require(Hmisc);
args <- commandArgs(TRUE);
input <- args[1];
output <- args[2];
pdf(paste(output,"/NR-cstrs-dur.pdf",sep=""), height=2, width=5)
# Trim off excess margin space (bottom, left, top, right)
par(mar=c(3.1, 3.9, 0.2, 0.5),ps=17,mgp=c(2,0.6,0))

dur <- read.table(paste(input,"/constraints-duration.data",sep=""),sep='\t',header=T)
sizes <- dur[,1];
plot(1,type="n",axes=F,xlab="",ylab="",xlim=c(15,30),ylim=c(0,20))

pchs <- c(1,2,3)
colors = gray(seq(0.8,0.1,length=3));
lwds <- c(3,3,3)

for (i in 7:9) {
    lines(sizes, dur[,i] - dur[,6], lwd=lwds[i-6], type="o", pch=pchs[i-6], col=colors[i-6])
}
axis(1,seq(15,30,by=5))
axis(2,seq(0,20,by=5),las=1)

title(,ylab="Time (sec)")
mtext("Virtual machines (x 1,000)",line=-9.5);

legend("topleft",c("33%","66%","100%"),col=colors,lwd=lwds,bty="n",pch=pchs,horiz=TRUE)
dev.off()