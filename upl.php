<?php


if (isset($_POST["submit"])) {
$updir = "/var/tmp/";
$upfile = $updir.basename($_FILES['rawexcel']['name']);

if(is_uploaded_file ($_FILES ["rawexcel"]["tmp_name"]))
{
move_uploaded_file ($_FILES["rawexcel"]["tmp_name"], $upfile);

} else {echo "error uploading file ".$upfile;}
} else {echo "not isset post method";}
?>
