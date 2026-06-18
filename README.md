# On-Together Bot

<img width="602" height="485" alt="imagen" src="https://github.com/user-attachments/assets/15d5faf1-75d6-413a-9f22-5097c8fa6f74" />

## Estructura de archivos

```
Application

bot         ├── Ene
            ├── Commands

bus         ├── Bus message
            ├── Reactive bus
    
discord     ├── Discord bot
            ├── Connection to Discord through Webhook for the on-together channel
            ├── Connection to Discord through Bot for the Minecraft channel
        
ontogether  ├── On-together bot
            ├── Message formatter for on-together
            
widget      ├── Server for the widget
            ├── Connection to the widget through Websocket
```