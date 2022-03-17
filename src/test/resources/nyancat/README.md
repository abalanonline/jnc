### 2011
* http://www.prguitarman.com/index.php?id=348
* http://www.prguitarman.com/comics/poptart1red1.gif

### 2021
* https://foundation.app/@NyanCat/foundation/219
* https://assets.foundation.app/es/wD/Qmcg8f4F9cig2JWXunxJcdBe58Q5myYXPmGfuMn1TVeswD/nft.mp4

```bash
ffmpeg -i poptart1red1.gif -vf palettegen palette.png
ffmpeg -i nft.mp4 -i palette.png -filter_complex "fps=14.29,scale=700:-1:flags=neighbor[x];[x][1:v]paletteuse=dither=none" nft.gif
```
