"""Leo the lion, the Maths Quest mascot: shared head plus one SVG fragment per expression (120x120 viewBox)."""
import math
def star(cx, cy, R, r=None, pts=5):
    r = r or R*0.45
    d=[]
    for k in range(pts*2):
        a = -math.pi/2 + k*math.pi/pts
        rad = R if k%2==0 else r
        d.append('%.1f %.1f'%(cx+rad*math.cos(a), cy+rad*math.sin(a)))
    return 'M'+'L'.join(d)+'z'
petals=''.join('<circle cx="%.1f" cy="%.1f" r="15" fill="#E07A1F"></circle>'%(60+40*math.cos(a*math.pi/6), 60+40*math.sin(a*math.pi/6)) for a in range(12))
BASE_TOP = petals + '''<circle cx="60" cy="60" r="42" fill="#F2A23A"></circle>
<circle cx="37" cy="36" r="9" fill="#FFC85C"></circle><circle cx="37" cy="36" r="4.5" fill="#F29E6B"></circle>
<circle cx="83" cy="36" r="9" fill="#FFC85C"></circle><circle cx="83" cy="36" r="4.5" fill="#F29E6B"></circle>
<circle cx="60" cy="62" r="30" fill="#FFC85C"></circle>
<circle cx="40" cy="70" r="5" fill="#FF8F7A" opacity="0.6"></circle><circle cx="80" cy="70" r="5" fill="#FF8F7A" opacity="0.6"></circle>
<ellipse cx="60" cy="74" rx="13" ry="10" fill="#FFF1D6"></ellipse>
<path d="M55 67.5h10q1.5 0 .6 1.3l-4.4 4.2q-1.2 1-2.4 0l-4.4-4.2q-.9-1.3.6-1.3z" fill="#7A3B1E"></path>
<path d="M44 14l7 9 9-13 9 13 7-9-2 17H46z" fill="#FFC21A" stroke="#C98A00" stroke-width="1.6" stroke-linejoin="round"></path>
<circle cx="60" cy="24" r="2.6" fill="#E8457F"></circle>
'''
INK='#14234B'; BROW='#A35A12'; MOUTH='#7A3B1E'
def stroke(d, c, w=2.2): return '<path d="%s" fill="none" stroke="%s" stroke-width="%s" stroke-linecap="round" stroke-linejoin="round"></path>'%(d,c,w)
def paw(cx, cy, rot=0): return '<ellipse cx="%s" cy="%s" rx="7.5" ry="6" fill="#FFC85C" stroke="#E0A040" stroke-width="1.2" transform="rotate(%s %s %s)"></ellipse>'%(cx,cy,rot,cx,cy)
def sparkle(cx, cy, R, c='#FFD43B'): return '<path d="%s" fill="%s" stroke="#C98A00" stroke-width="0.8" stroke-linejoin="round"></path>'%(star(cx,cy,R,R*0.38,4), c)
BIG_EYES = ('<ellipse cx="49" cy="58" rx="5" ry="6.2" fill="#14234B"></ellipse><circle cx="50.8" cy="55.6" r="2" fill="#FFFFFF"></circle><circle cx="47.6" cy="60.6" r="1" fill="#FFFFFF"></circle>'
            '<ellipse cx="71" cy="58" rx="5" ry="6.2" fill="#14234B"></ellipse><circle cx="72.8" cy="55.6" r="2" fill="#FFFFFF"></circle><circle cx="69.6" cy="60.6" r="1" fill="#FFFFFF"></circle>')
OPEN_MOUTH = '<path d="M51 76.5q9 13 18 0z" fill="#7A3B1E"></path><ellipse cx="60" cy="83" rx="4.5" ry="2.6" fill="#FF8F7A"></ellipse>'
OPEN_EYES = '<ellipse cx="49" cy="58" rx="4.2" ry="5.2" fill="#14234B"></ellipse><circle cx="50.4" cy="56.2" r="1.5" fill="#FFFFFF"></circle><ellipse cx="71" cy="58" rx="4.2" ry="5.2" fill="#14234B"></ellipse><circle cx="72.4" cy="56.2" r="1.5" fill="#FFFFFF"></circle>'
COIN = ('<circle cx="96" cy="94" r="14" fill="#FFC21A" stroke="#C98A00" stroke-width="2"></circle>'
        '<path d="M96 86.5l2.2 4.5 5 .7-3.6 3.5.9 5-4.5-2.4-4.5 2.4.9-5-3.6-3.5 5-.7z" fill="#FFE27A" stroke="#C98A00" stroke-width="1.2" stroke-linejoin="round"></path>'
        '<ellipse cx="86" cy="100" rx="7" ry="5.5" fill="#FFC85C" stroke="#E0A040" stroke-width="1.2"></ellipse>')
FACES = {
 # Reward Shop: hopeful, big shiny eyes, paws clasped, a little heart
 'hopeful': stroke('M44 49q5-4.5 10-3M66 46q5-1.5 10 3', BROW, 2.4) + BIG_EYES
   + stroke('M60 73.5v2.5', MOUTH) + '<ellipse cx="60" cy="79.5" rx="3.6" ry="3" fill="#7A3B1E"></ellipse>'
   + paw(55, 93, 25) + paw(65, 93, -25)
   + '<path d="M102 26c-3-4-9-1.5-6.5 3.5L102 35l6.5-5.5c2.5-5-3.5-7.5-6.5-3.5z" fill="#E8457F"></path>' + sparkle(16, 34, 5),
 # Daily Challenge (to do): excited, holding a calendar
 'excited': stroke('M43 45q5-5 11-2M66 43q6-3 11 2', BROW, 2.4) + BIG_EYES
   + stroke('M60 72v2', MOUTH) + OPEN_MOUTH
   + '<rect x="86" y="80" width="24" height="22" rx="3" fill="#FFFFFF" stroke="#C42E6E" stroke-width="1.6"></rect><path d="M86 86.5h24V83a3 3 0 0 0-3-3H89a3 3 0 0 0-3 3z" fill="#E8457F"></path>'
   + '<path d="%s" fill="#FFC21A" stroke="#C98A00" stroke-width=".8" stroke-linejoin="round"></path>'%star(98,94,5.5)
   + paw(86, 98, -10) + sparkle(12, 40, 5) + sparkle(108, 64, 4.5, '#FFFFFF'),
 # Daily Challenge (done): sleepy, yawning, floating Zs
 'sleepy': stroke('M44 51q5-2 10 0M66 51q5-2 10 0', BROW, 2.2)
   + stroke('M45 58.5q4 3.5 8 0M67 58.5q4 3.5 8 0', INK, 2.6)
   + stroke('M60 73.5v1.5', MOUTH) + '<ellipse cx="60" cy="80" rx="4" ry="4.6" fill="#7A3B1E"></ellipse>'
   + paw(48, 92, 20)
   + stroke('M90 30h7l-7 8h7', '#1F74E0', 2.2) + stroke('M101 16h9l-9 10h9', '#1F74E0', 2.6) + stroke('M113 2h6l-6 7h6', '#1F74E0', 1.8),
}
def lion(face, px, label):
    return ('<svg class="mq-bob" width="%d" height="%d" viewBox="0 0 120 120" role="img" aria-label="%s" style="flex: none; overflow: visible">\n'%(px,px,label)
            + BASE_TOP + FACES[face] + '\n</svg>')
# My Progress: proud coach, winking, thumbs-up, holding a rising bar chart
FACES['coach'] = (stroke('M44 49q5-4 10 0M66 47q5-4.5 10 0', BROW, 2.4)
   + stroke('M45 58.5q4-3.5 8 0', INK, 2.8)
   + '<ellipse cx="71" cy="58" rx="4.4" ry="5.4" fill="#14234B"></ellipse><circle cx="72.5" cy="56.2" r="1.6" fill="#FFFFFF"></circle>'
   + stroke('M60 73.5v2.5M52.5 76.5q7.5 7.5 15 0', MOUTH)
   + '<rect x="84" y="78" width="28" height="24" rx="3" fill="#FFFFFF" stroke="#1F74E0" stroke-width="1.6"></rect>'
   + '<rect x="88" y="92" width="5" height="6" fill="#1C9A44"></rect><rect x="95.5" y="87" width="5" height="11" fill="#6A3FD8"></rect><rect x="103" y="82" width="5" height="16" fill="#D9560B"></rect>'
   + paw(84, 98, -10)
   + '<ellipse cx="20" cy="88" rx="7" ry="8" fill="#FFC85C" stroke="#E0A040" stroke-width="1.2"></ellipse><rect x="16.5" y="73" width="6" height="12" rx="3" fill="#FFC85C" stroke="#E0A040" stroke-width="1.2"></rect>'
   + sparkle(108, 64, 4.5))
OPEN_EYES = '<ellipse cx="49" cy="58" rx="4.2" ry="5.2" fill="#14234B"></ellipse><circle cx="50.4" cy="56.2" r="1.5" fill="#FFFFFF"></circle><ellipse cx="71" cy="58" rx="4.2" ry="5.2" fill="#14234B"></ellipse><circle cx="72.4" cy="56.2" r="1.5" fill="#FFFFFF"></circle>'
# Grade: friendly wave
FACES['waving'] = (stroke('M44 48q5-4.5 10-1M66 47q5-3.5 10 1', BROW, 2.4) + OPEN_EYES
   + stroke('M60 73.5v2.5M52.5 76.5q7.5 7.5 15 0', MOUTH)
   + paw(14, 44, -25) + stroke('M3 30q-3 6 0 12M8 24q-4 4-3 9', '#1F74E0', 1.8)
   + sparkle(106, 30, 5))
# Topic: curious, eyebrows up, looking down at the list and pointing
FACES['pointing'] = (stroke('M44 46q5-5 10-2M66 44q5-3 10 2', BROW, 2.4)
   + '<ellipse cx="49" cy="60" rx="4" ry="4.8" fill="#14234B"></ellipse><circle cx="50" cy="61.8" r="1.4" fill="#FFFFFF"></circle><ellipse cx="71" cy="60" rx="4" ry="4.8" fill="#14234B"></ellipse><circle cx="72" cy="61.8" r="1.4" fill="#FFFFFF"></circle>'
   + stroke('M60 73.5v2.5', MOUTH) + '<path d="M55 78.5q5 5 10 0z" fill="#7A3B1E"></path>'
   + paw(96, 96, 40) + '<rect x="101" y="100" width="5" height="12" rx="2.5" fill="#FFC85C" stroke="#E0A040" stroke-width="1.2" transform="rotate(-40 103.5 106)"></rect>'
   + stroke('M110 112l5 5M116 107l5 2', '#1F74E0', 1.8))
# Difficulty: ready to go, determined grin, fist pump
FACES['ready'] = (stroke('M43.5 49l10.5 3M76.5 49l-10.5 3', BROW, 2.6) + OPEN_EYES
   + stroke('M60 72v2', MOUTH) + '<path d="M50.5 76q9.5 11 19 0z" fill="#7A3B1E"></path><path d="M51.5 76.6h17l-1.2 2.4h-14.6z" fill="#FFFFFF"></path>'
   + paw(104, 40, 0) + stroke('M100 34v-4M104 33v-5M108 34v-4', '#E0A040', 1.4)
   + stroke('M112 22l4-5M116 30l6-1M98 22l-2-6', '#D9560B', 2) + sparkle(12, 36, 5))
# Profile picker: welcoming, arms wide open
FACES['welcome'] = (stroke('M44 47q5-4.5 10-1M66 46q5-3.5 10 1', BROW, 2.4) + BIG_EYES
   + stroke('M60 72v2', MOUTH) + OPEN_MOUTH
   + paw(10, 76, -40) + paw(110, 76, 40)
   + sparkle(8, 50, 5) + sparkle(112, 48, 5) + sparkle(104, 104, 4, '#FFFFFF'))
# PIN: paws over his eyes so he can't peek
def big_paw(cx, cy, rot):
    return ('<g transform="rotate(%s %s %s)"><ellipse cx="%s" cy="%s" rx="10.5" ry="8.5" fill="#FFC85C" stroke="#E0A040" stroke-width="1.3"></ellipse>'%(rot,cx,cy,cx,cy)
            + stroke('M%s %sv5M%s %sv6M%s %sv5'%(cx-4.5,cy-8,cx,cy-8.5,cx+4.5,cy-8), '#E0A040', 1.2) + '</g>')
FACES['covering'] = (stroke('M43 44q5-3 10-1M67 43q5-2 10 1', BROW, 2.4)
   + stroke('M60 73.5v2.5', MOUTH) + '<path d="M54.5 78.5q5.5 5 11 0z" fill="#7A3B1E"></path>'
   + big_paw(48, 57, -12) + big_paw(72, 57, 12)
   + '<rect x="94" y="84" width="20" height="16" rx="3" fill="#1F74E0"></rect>' + stroke('M98 84v-4a6 6 0 0 1 12 0v4', '#1F74E0', 2.6)
   + '<circle cx="104" cy="91" r="2.2" fill="#FFFFFF"></circle><rect x="103" y="92" width="2" height="4" fill="#FFFFFF"></rect>')
# Parent dashboard: calm reporter with reading glasses and a clipboard
FACES['reporter'] = (stroke('M44 48q5-3 10-1M66 47q5-2 10 1', BROW, 2.2)
   + '<ellipse cx="49" cy="58.5" rx="3.4" ry="4.2" fill="#14234B"></ellipse><circle cx="50.2" cy="57.2" r="1.2" fill="#FFFFFF"></circle><ellipse cx="71" cy="58.5" rx="3.4" ry="4.2" fill="#14234B"></ellipse><circle cx="72.2" cy="57.2" r="1.2" fill="#FFFFFF"></circle>'
   + '<circle cx="49" cy="58.5" r="8" fill="none" stroke="#14234B" stroke-width="2"></circle><circle cx="71" cy="58.5" r="8" fill="none" stroke="#14234B" stroke-width="2"></circle>'
   + stroke('M57 58.5h6M41 57l-7-3M79 57l7-3', '#14234B', 2)
   + stroke('M60 73.5v2.5M54 77q6 5 12 0', MOUTH)
   + '<rect x="86" y="74" width="24" height="30" rx="3" fill="#FFFFFF" stroke="#4A5A80" stroke-width="1.6"></rect><rect x="93" y="71" width="10" height="6" rx="2" fill="#4A5A80"></rect>'
   + stroke('M90 84h16M90 90h16M90 96h10', '#9AA8C7', 1.6) + stroke('M101 95l2.5 2.5 4.5-5', '#1C9A44', 2)
   + paw(86, 100, -10))

# --- The first five expressions ---
# Home: friendly smile, holding a Gold Coin
FACES['happy'] = (stroke('M44 49q5-4 10 0M66 49q5-4 10 0', BROW) + OPEN_EYES
   + stroke('M60 73.5v2.5M53.5 77q6.5 6 13 0', MOUTH) + COIN)
# Question: focused, pencil at the ready
FACES['focused'] = (stroke('M43.5 50.5l10.5 2.2M76.5 50.5l-10.5 2.2', BROW, 2.6)
   + '<ellipse cx="49" cy="58.5" rx="4" ry="4.6" fill="#14234B"></ellipse><circle cx="50.2" cy="57" r="1.4" fill="#FFFFFF"></circle><ellipse cx="71" cy="58.5" rx="4" ry="4.6" fill="#14234B"></ellipse><circle cx="72.2" cy="57" r="1.4" fill="#FFFFFF"></circle>'
   + stroke('M60 73.5v2.5M55 78q5 3.5 10 0', MOUTH)
   + '<g transform="rotate(35 96 92)"><rect x="92" y="72" width="8" height="26" rx="1.5" fill="#1F74E0"></rect><rect x="92" y="72" width="8" height="5" rx="1.5" fill="#FF8F7A"></rect><path d="M92 98h8l-4 8z" fill="#FFE0B0"></path><path d="M94.6 103.2h2.8L96 106z" fill="#14234B"></path></g>'
   + paw(88, 98, -20))
# Almost: thinking, paw on chin, thought bubbles with a question mark
FACES['thinking'] = (stroke('M44 47.5q5-6 10-2M66 51.5h10', BROW, 2.4)
   + '<ellipse cx="51" cy="56.5" rx="3.6" ry="4.6" fill="#14234B"></ellipse><circle cx="52" cy="55" r="1.3" fill="#FFFFFF"></circle><ellipse cx="73" cy="56.5" rx="3.6" ry="4.6" fill="#14234B"></ellipse><circle cx="74" cy="55" r="1.3" fill="#FFFFFF"></circle>'
   + stroke('M60 73.5v2.5M56 79.5q4-2.5 8.5-.5', MOUTH)
   + paw(68, 92, 15)
   + '<circle cx="98" cy="30" r="3" fill="#FFFFFF" stroke="#BFD9F5" stroke-width="1.2"></circle><circle cx="106" cy="20" r="4.5" fill="#FFFFFF" stroke="#BFD9F5" stroke-width="1.2"></circle><circle cx="116" cy="6" r="7" fill="#FFFFFF" stroke="#BFD9F5" stroke-width="1.2"></circle>'
   + stroke('M113.6 4.2q.6-2.6 2.8-2.6 2.6 0 2.6 2.3 0 1.6-2.2 2.6v1.4', '#1F74E0', 1.6) + '<circle cx="116.6" cy="10.3" r="0.9" fill="#1F74E0"></circle>')
# Correct: cheering with paws up and sparkles
FACES['cheering'] = (stroke('M44 46q5-5 10-1M66 45q5-4 10 1', BROW, 2.4)
   + stroke('M44.5 59q4.5-6 9 0M66.5 59q4.5-6 9 0', INK, 2.8)
   + stroke('M60 72v2', MOUTH) + OPEN_MOUTH
   + paw(14, 54, -30) + paw(106, 54, 30)
   + sparkle(8, 32, 6) + sparkle(112, 30, 6) + sparkle(100, 100, 5, '#FFFFFF') + sparkle(18, 98, 4.5, '#FFFFFF'))
# Quest complete: proud, star eyes, holding the trophy
FACES['proud'] = (stroke('M44 47q5-4.5 10 0M66 47q5-4.5 10 0', BROW, 2.4)
   + '<path d="%s" fill="#FFC21A" stroke="#14234B" stroke-width="1.4" stroke-linejoin="round"></path>' % star(49, 58, 6.5)
   + '<path d="%s" fill="#FFC21A" stroke="#14234B" stroke-width="1.4" stroke-linejoin="round"></path>' % star(71, 58, 6.5)
   + stroke('M60 72v2', MOUTH) + OPEN_MOUTH
   + '<path d="M88 84h18v6a9 9 0 0 1-18 0z" fill="#FFC21A" stroke="#C98A00" stroke-width="1.4" stroke-linejoin="round"></path><path d="M88 86h-3.5v1.5a4 4 0 0 0 4 4M106 86h3.5v1.5a4 4 0 0 1-4 4" fill="none" stroke="#C98A00" stroke-width="1.4"></path><rect x="95" y="98" width="4" height="5" fill="#FFC21A" stroke="#C98A00" stroke-width="1.2"></rect><rect x="91" y="103" width="12" height="4" rx="1" fill="#1F74E0"></rect>'
   + paw(86, 101, -10) + sparkle(110, 74, 5) + sparkle(14, 36, 5))
