import { Button } from "@delta/ui";
import { useEffect, useMemo, useRef, useState } from "react";

export type WechatPayDialogPayload = {
  orderNo: string;
  paymentMethod: "WECHAT" | "ALIPAY";
  tradeType: "NATIVE" | "JSAPI";
  codeUrl: string;
  expireAt: string;
  jsapiPayParams: {
    appId: string;
    timeStamp: string;
    nonceStr: string;
    packageValue: string;
    signType: string;
    paySign: string;
  } | null;
};

export function WechatPayDialog({
  open,
  title,
  payload,
  onClose,
  onCheckPaid,
  onPaid,
}: {
  open: boolean;
  title: string;
  payload: WechatPayDialogPayload | null;
  onClose: () => void;
  onCheckPaid: () => Promise<boolean>;
  onPaid: () => Promise<void> | void;
}) {
  const [notice, setNotice] = useState("");
  const [paying, setPaying] = useState(false);
  const [checking, setChecking] = useState(false);
  const jsapiTriggeredRef = useRef(false);

  const qrImageUrl = useMemo(() => {
    if (!payload?.codeUrl) {
      return "";
    }
    return `https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=${encodeURIComponent(payload.codeUrl)}`;
  }, [payload?.codeUrl]);

  useEffect(() => {
    if (!open || !payload || payload.tradeType !== "NATIVE" || payload.paymentMethod !== "WECHAT") {
      return;
    }
    const timer = window.setInterval(() => {
      void checkPaid();
    }, 3000);
    return () => window.clearInterval(timer);
  }, [open, payload?.orderNo, payload?.tradeType]);

  useEffect(() => {
    if (!open || !payload || payload.paymentMethod !== "WECHAT" || payload.tradeType !== "JSAPI" || !payload.jsapiPayParams || jsapiTriggeredRef.current) {
      return;
    }
    jsapiTriggeredRef.current = true;
    void invokeJsapi();
  }, [open, payload]);

  useEffect(() => {
    if (!open) {
      setNotice("");
      setPaying(false);
      setChecking(false);
      jsapiTriggeredRef.current = false;
    }
  }, [open]);

  if (!open || !payload) {
    return null;
  }

  const currentPayload = payload;
  const paymentLabel = currentPayload.paymentMethod === "ALIPAY" ? "支付宝" : "微信";

  async function invokeJsapi() {
    if (!currentPayload.jsapiPayParams) {
      return;
    }
    try {
      setPaying(true);
      setNotice("正在唤起微信支付...");
      await invokeWechatBridge(currentPayload.jsapiPayParams);
      setNotice("支付已提交，正在等待平台确认到账...");
      const paid = await onCheckPaid();
      if (paid) {
        await onPaid();
        onClose();
        return;
      }
      setNotice("微信支付已完成，平台暂未确认到账，请稍后点击检查到账。");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "微信支付未完成，请稍后重试");
    } finally {
      setPaying(false);
    }
  }

  async function checkPaid() {
    if (checking) {
      return;
    }
    try {
      setChecking(true);
      const paid = await onCheckPaid();
      if (paid) {
        setNotice("订单已支付成功。");
        await onPaid();
        onClose();
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "订单状态刷新失败");
    } finally {
      setChecking(false);
    }
  }

  async function confirmPaid() {
    try {
      setChecking(true);
      const paid = await onCheckPaid();
      if (!paid) {
        setNotice("平台暂未确认到账，请稍后再试。");
        return;
      }
      setNotice("订单已支付成功。");
      await onPaid();
      onClose();
    } catch (error) {
      setNotice(error instanceof Error ? error.message : `${paymentLabel}支付确认失败`);
    } finally {
      setChecking(false);
    }
  }

  return (
    <div className="wechat-pay-dialog" role="dialog" aria-modal="true" aria-label={title}>
      <button className="wechat-pay-dialog__backdrop" type="button" onClick={onClose} />
      <section className="wechat-pay-dialog__panel">
        <header className="wechat-pay-dialog__header">
          <div>
            <h3>{title}</h3>
            <p>订单号 {payload.orderNo}</p>
          </div>
          <button className="wechat-pay-dialog__close" type="button" onClick={onClose}>
            关闭
          </button>
        </header>
        <div className="wechat-pay-dialog__body">
          {payload.tradeType === "JSAPI" ? (
            <div className="wechat-pay-dialog__state">
              <strong>已切换到公众号支付</strong>
              <p>如果没有自动拉起支付，请点击下方按钮重新发起。</p>
              <Button disabled={paying} onClick={() => void invokeJsapi()}>
                {paying ? "拉起中..." : "重新唤起微信支付"}
              </Button>
            </div>
          ) : (
            <>
              <div className="wechat-pay-dialog__qr">
                <img alt={`${paymentLabel}支付二维码`} src={qrImageUrl} />
              </div>
              <div className="wechat-pay-dialog__meta">
                <strong>请使用{paymentLabel}扫码支付</strong>
                <p>
                  {currentPayload.paymentMethod === "WECHAT"
                    ? "二维码会自动轮询订单状态，支付成功后会自动关闭当前弹层。"
                    : "完成扫码支付后，点击下方按钮确认到账。"}
                </p>
                <span>有效期至 {formatTimestamp(currentPayload.expireAt)}</span>
              </div>
              <div className="wechat-pay-dialog__actions">
                <Button disabled={checking} onClick={() => void (currentPayload.paymentMethod === "ALIPAY" ? confirmPaid() : checkPaid())}>
                  {checking ? "检查中..." : "检查到账"}
                </Button>
              </div>
            </>
          )}
          {notice ? <p className="wechat-pay-dialog__notice">{notice}</p> : null}
        </div>
      </section>
    </div>
  );
}

function invokeWechatBridge(params: NonNullable<WechatPayDialogPayload["jsapiPayParams"]>) {
  return new Promise<void>((resolve, reject) => {
    if (typeof window === "undefined") {
      reject(new Error("当前环境不支持微信支付"));
      return;
    }
    const onReady = () => {
      const bridge = window.WeixinJSBridge;
      if (!bridge) {
        reject(new Error("当前浏览器未注入微信支付能力"));
        return;
      }
      bridge.invoke(
        "getBrandWCPayRequest",
        {
          appId: params.appId,
          timeStamp: params.timeStamp,
          nonceStr: params.nonceStr,
          package: params.packageValue,
          signType: params.signType,
          paySign: params.paySign,
        },
        (response: { err_msg?: string }) => {
          if (response?.err_msg === "get_brand_wcpay_request:ok") {
            resolve();
            return;
          }
          reject(new Error("微信支付未完成，请确认支付或稍后重试"));
        },
      );
    };
    if (window.WeixinJSBridge) {
      onReady();
      return;
    }
    document.addEventListener("WeixinJSBridgeReady", onReady, { once: true });
  });
}

function formatTimestamp(value: string) {
  if (!value) {
    return "—";
  }
  return value.replace("T", " ").slice(0, 16);
}

declare global {
  interface Window {
    WeixinJSBridge?: {
      invoke: (name: string, payload: Record<string, string>, callback: (response: { err_msg?: string }) => void) => void;
    };
  }
}
