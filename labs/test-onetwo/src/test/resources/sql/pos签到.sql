USE [ICCARD]
GO
/****** Object:  StoredProcedure [dbo].[PROC_SIGN_IN]    Script Date: 01/13/2014 11:12:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--POS�ն�ǩ��
ALTER PROCEDURE [dbo].[PROC_SIGN_IN](
       --�������
       @IN_PID VARCHAR(20),			--�ն˻����
       @IN_CBLACK_VER VARCHAR(8),	--�ն˺������汾
       @IN_TIM CHAR(14),			--�ն�����ʱ��
       @IN_APP_VERSION	VARCHAR(16),
       --�������
       @OUT_RET CHAR(4) OUTPUT,		--������ ��0000���ɹ�  ����ֵʧ��
       @OUT_PID CHAR(12) OUTPUT,	--�ն˻����
       @OUT_TIM CHAR(14) OUTPUT,	--������ʱ��
       @OUT_SBLACK_VER VARCHAR(8) OUTPUT,	--�������������汾
       @OUT_MERCHANT   VARCHAR(40) OUTPUT	--�̻�����
) AS 
BEGIN 
  BEGIN TRY
  DECLARE 
		  @V_PID VARCHAR(20),
		  @MAX_VER BIGINT,		--����@MAX_VER����ߺ������汾
		  @V_PRODUCT_NUM VARCHAR(20)='';--������Ʒ���
	  SET @OUT_RET='0000';--Ĭ�ϳɹ�
	  SET @OUT_MERCHANT='';
	  SET @OUT_SBLACK_VER=@IN_CBLACK_VER;	--��ʼ��@OUT_SBLACK_VER
	  SET @OUT_PID=@IN_PID;		--��������ն˻���ŵ��������ն˻����
	  SET @OUT_TIM=DBO.DATETIMETOSTR(); 
	  
	  --����pos��Ų�ѯ���̻�ID���Եõ��̻�����
	  DECLARE @V_MERCHANT_ID BIGINT=0;
	  IF(CAST(@IN_PID AS BIGINT) >= 100000000)
		BEGIN
			SET @V_PID='00000000'+@IN_PID; --��ֵ�ն˺�
			SELECT @V_MERCHANT_ID=MERCHANT_ID FROM TERM_RECHARGE_POS WHERE RECHARGE_POS_NO=@V_PID;
		END
	  ELSE
		BEGIN
			SET @V_PID=@IN_PID; --�����ն˺�
			SELECT @V_MERCHANT_ID=MERCHANT_ID FROM TERM_POS WHERE POS_NO=@V_PID; 
		END
	  SELECT @OUT_MERCHANT=NAME FROM MERCHANT_INFO WHERE ID=@V_MERCHANT_ID;
	  
	  --δ�ҵ��̻�  ���ء�1000�� ʧ��
	  IF(@OUT_MERCHANT IS NULL OR @OUT_MERCHANT='')
		BEGIN
			 SET @OUT_RET='FF03';
			 RETURN -1;
		END
	  --�ҵ��̻� ���̻���LAST_UPDATE_TIME����
	  ELSE
		BEGIN
			--zws 2014-05-25:ȥ�������������update
		  --UPDATE TERM_POS SET LAST_UPDATE_TIME=SYSDATETIME() WHERE TERM_POS.POS_NO=@V_PID;
		  --��ѯ�̻��󶨵���Ʒ,��û������Ϊ''
		  SELECT @V_PRODUCT_NUM=@V_PRODUCT_NUM+PRODUCT_NUM FROM TRAVEL_PRODUCT WHERE MERCHANT_ID = @V_MERCHANT_ID;
			IF(@V_PRODUCT_NUM IS NULL)
				SET @V_PRODUCT_NUM='';	  
		  
		  SELECT @MAX_VER=MAX(VERSION_ID) FROM CLIENT_BLACK_LIST;	--��ѯ����ߺ������汾 ����@MAX_VER
		  IF(@MAX_VER=@IN_CBLACK_VER OR @MAX_VER IS NULL)	--����ն˺������汾������߰汾����߰汾Ϊ��
			BEGIN											--���ط������������汾=�ն˺������汾
				SET @OUT_SBLACK_VER=@IN_CBLACK_VER;
			END
		  ELSE IF(@MAX_VER>@IN_CBLACK_VER)		--�ն˺������汾С���ն˺������汾
			BEGIN
				SET @OUT_SBLACK_VER=@MAX_VER;
			END;
		  
		  --��¼�ն˺������汾�ź����ݿ���߰汾�ŵ�term_pos_blacklist_ver
		  BEGIN TRANSACTION
			 --���̻���LAST_UPDATE_TIME����
				--zws 2014-05-25:ȥ�����update
			  --UPDATE TERM_POS SET LAST_UPDATE_TIME=SYSDATETIME() WHERE TERM_POS.POS_NO=@V_PID;
			  --��¼�ն˺������汾�ź����ݿ���߰汾�ŵ�term_pos_blacklist_ver
			  DECLARE @V_APP_VERSION VARCHAR(16)='';
			  --��ȡ���и��ն˳���汾��
			  SELECT @V_APP_VERSION=pos_app_version FROM term_pos_blacklist_ver WHERE PID=@IN_PID;
			  --����δ����
			  IF(@V_APP_VERSION = @IN_APP_VERSION)
				UPDATE term_pos_blacklist_ver SET pos_version=@IN_CBLACK_VER, signin_time=SYSDATETIME(), server_version=@OUT_SBLACK_VER WHERE PID=@IN_PID;
			  --����������
			  ELSE
				UPDATE term_pos_blacklist_ver SET pos_version=@IN_CBLACK_VER, signin_time=SYSDATETIME(), server_version=@OUT_SBLACK_VER, pos_app_version=@IN_APP_VERSION, app_time=SYSDATETIME() WHERE PID=@IN_PID;
			  --����û�и��ն˵ļ�¼
			  IF(@@ROWCOUNT = 0)
				INSERT INTO term_pos_blacklist_ver(PID, pos_version, signin_time, server_version, pos_app_version) VALUES(@IN_PID, @IN_CBLACK_VER, SYSDATETIME(), @OUT_SBLACK_VER ,@IN_APP_VERSION);
		  COMMIT TRANSACTION;
		 
		  IF @@ERROR<>0  --����ʧ�� ���ء�2000��
			BEGIN  
				SET @OUT_RET='FF02';
				ROLLBACK TRANSACTION;
				RETURN -2;  
			END	
		
		  --���³ɹ�  ���ء�0000�� �ɹ�
		  SET @OUT_RET='0000';
		  --����̻��������β�Ʒ���
		  SET @OUT_MERCHANT=@OUT_MERCHANT+'\n'+@V_PRODUCT_NUM;
		  RETURN 0;
		END
	RETURN 0;
  END TRY
  BEGIN CATCH    --�쳣���� ���ء�3000�� ʧ��
	  IF @@TRANCOUNT<>0
	  BEGIN
		ROLLBACK TRANSACTION;
	  END
	  SET @OUT_RET='FF01';
	  RETURN -3;
  END CATCH
END;
