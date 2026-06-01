import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";

export type ImRealtimeState = "connecting" | "connected" | "reconnecting" | "error" | "closed";

type TopicSubscription = {
  topic: string;
  onMessage: (payload: unknown) => void;
};

type RealtimeOptions = {
  subscriptions: TopicSubscription[];
  onStateChange?: (state: ImRealtimeState) => void;
};

export function createImRealtimeSession(options: RealtimeOptions) {
  let subscriptions: StompSubscription[] = [];

  const client = new Client({
    brokerURL: resolveBrokerUrl(),
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect() {
      options.onStateChange?.("connected");
      subscriptions = options.subscriptions.map((item) =>
        client.subscribe(item.topic, (message) => {
          item.onMessage(parsePayload(message));
        }),
      );
    },
    onStompError() {
      options.onStateChange?.("error");
    },
    onWebSocketClose() {
      options.onStateChange?.("reconnecting");
    },
    onWebSocketError() {
      options.onStateChange?.("error");
    },
    beforeConnect() {
      options.onStateChange?.("connecting");
    },
  });

  client.activate();

  return () => {
    subscriptions.forEach((item) => item.unsubscribe());
    subscriptions = [];
    options.onStateChange?.("closed");
    void client.deactivate();
  };
}

function resolveBrokerUrl() {
  if (typeof window === "undefined") {
    return "ws://127.0.0.1:8080/ws-im";
  }
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const localHostnames = new Set(["localhost", "127.0.0.1"]);
  const host = localHostnames.has(window.location.hostname)
    ? `${window.location.hostname}:8080`
    : window.location.host;
  return `${protocol}//${host}/ws-im`;
}

function parsePayload(message: IMessage) {
  try {
    return JSON.parse(message.body) as unknown;
  } catch {
    return message.body;
  }
}
