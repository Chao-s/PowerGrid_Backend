// 更新节点的 isauto 属性
match (n:DMS_CB_DEVICE) set n.isAuto = false;
// 根据导入的文件写
using periodic commit 500
load csv with headers from 'file:///decloud_autodevice_account.csv' as line
match (n) where n.id = line[0]
return n limit 1000;