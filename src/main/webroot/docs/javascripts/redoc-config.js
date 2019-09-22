/* Theme details: https://github.com/Redocly/redoc/blob/master/src/theme.ts */

/* UCLA theme (courtesy of Josh's Slack theme colors):
 *  #8BB8E8 (light blue)
 *  #2774AE (medium light blue)
 *  #FFB81C (yellow orange)
 *  #005587 (medium dark blue)
 *  #FFD100 (light yellow)
 *  #003B5C (dark blue)
 *  #00FF87 (green)
 *  #FF00A5 (pink)
 */

Redoc.init('/docs/manifeststore.yaml', {
  theme : {
    colors : {
      primary : {
        main : '#003B5C'
      },
      http : {
        get: '#6bbd5b',
        post: '#FFB81C', /*'#248fb2',*/
        put: '#9b708b',
        options: '#d3ca12',
        patch: '#e09d43',
        delete: '#e27a7a',
        basic: '#999',
        link: '#31bbb6',
        head: '#c167e4',
      }
    },
    typography : {
      code : {
        color : '#FFD100',
        backgroundColor : '#FFD100'
      }
    },
    rightPanel : {
      backgroundColor : '#8BB8E8',
      width : '30%'
    },
    codeSample : {
      backgroundColor : '#003B5C'
    }
  }
}, document.getElementById('manifest-store-docs'))
