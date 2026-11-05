"""Import whole-turkey icon art and generate exact block geometry/data. Requires Pillow."""
from pathlib import Path
import json
import copy
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/turkeytime'

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

for name, source in (
    ('raw_turkey', ROOT/'art/whole-turkey/raw.png'),
    ('cooked_turkey', ROOT/'art/whole-turkey/cooked.png'),
    ('turkey_egg', ROOT/'art/turkey-egg/egg.png'),
):
    # Preserve source artwork and alpha when resizing.
    with Image.open(source) as original:
        original = original.convert('RGBA')
        bounds = original.getchannel('A').getbbox()
        sprite = original.crop(bounds)
        sprite.thumbnail((30, 30), Image.Resampling.NEAREST)
        im = Image.new('RGBA', (32, 32))
        im.alpha_composite(sprite, ((32-sprite.width)//2, (32-sprite.height)//2))
    path=ASSETS/'textures/item'/f'{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)

for item in ('raw_turkey','cooked_turkey','turkey_egg'):
    write(ASSETS/'items'/f'{item}.json', {'model':{'type':'minecraft:model','model':f'turkeytime:item/{item}'}})
    write(ASSETS/'models/item'/f'{item}.json', {'parent':'minecraft:item/generated','textures':{'layer0':f'turkeytime:item/{item}'}})

for recipe in ('cooked_turkey','cooked_turkey_from_smoking','cooked_turkey_from_campfire_cooking'):
    write(RES/'data/turkeytime/advancement/recipes/food'/f'{recipe}.json', {
        'parent':'minecraft:recipes/root',
        'criteria':{
            'has_raw_turkey':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'turkeytime:raw_turkey'}]}},
            'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipes':f'turkeytime:{recipe}'}}
        },
        'requirements':[['has_raw_turkey','has_recipe']],
        'rewards':{'recipes':[f'turkeytime:{recipe}']}
    })

# Code-authored exact block textures and whole-roast geometry, independent of icon art.
block_dir = ASSETS/'textures/block'
block_dir.mkdir(parents=True, exist_ok=True)
for name, base in [('turkey_skin',(160,78,29)),('turkey_meat',(229,193,151)),('turkey_bone',(230,217,183))]:
    im = Image.new('RGB',(16,16),base); px=im.load()
    for y in range(16):
        for x in range(16):
            delta = 20 if (x*7+y*11)%13==0 else -13 if (x*3+y*5)%11==0 else 0
            px[x,y]=tuple(max(0,min(255,c+delta)) for c in base)
    im.save(block_dir/f'{name}.png')

# from/to, material: stepped breast, two wings, two drumsticks/bones, rear tail.
parts = [
    ([3,1,4],[13,3,12],'skin'), ([2,3,4],[14,6,12],'skin'),
    ([3,6,5],[13,8.5,12],'skin'), ([5,8.5,6],[11,10,11],'skin'),
    ([3,3,3],[13,7,5],'skin'),
    ([2.5,1,1.5],[6,4,5.5],'skin'), ([10,1,1.5],[13.5,4,5.5],'skin'),
    ([3.5,1.8,1],[4.8,3,2.3],'bone'), ([11.2,1.8,1],[12.5,3,2.3],'bone'),
    ([1,2,7],[3,5,11],'skin'), ([13,2,7],[15,5,11],'skin'),
    ([1.5,4.5,8],[4,6,11.5],'skin'), ([12,4.5,8],[14.5,6,11.5],'skin'),
    ([6,1,12],[10,4,15],'skin')
]
# Approximate equal portions rather than removing only a tiny wing on bite one.
# These clipping planes match CookedTurkeyBlock's shrinking interaction box.
cuts = [1,4,5.5,6.75,8,9.25,10.5,12]
for bite,cut in enumerate(cuts):
    elements=[]
    for low,high,material in parts:
        if high[0] <= cut: continue
        clipped=[max(cut,low[0]),low[1],low[2]]
        faces={side:{'texture':f'#{material}'} for side in ('up','down','north','south','west','east')}
        if bite and low[0]<cut and material=='skin': faces['west']['texture']='#meat'
        elements.append({'from':clipped,'to':high,'faces':faces})
    write(ASSETS/'models/block'/f'cooked_turkey_{bite}.json',{
        'textures':{'skin':'turkeytime:block/turkey_skin','meat':'turkeytime:block/turkey_meat',
                    'bone':'turkeytime:block/turkey_bone','particle':'turkeytime:block/turkey_skin'},
        'elements':elements
    })
variants={}
for bite in range(8):
    for facing,y in [('north',0),('east',90),('south',180),('west',270)]:
        variant={'model':f'turkeytime:block/cooked_turkey_{bite}'}
        if y: variant['y']=y
        variants[f'bites={bite},facing={facing}']=variant
write(ASSETS/'blockstates/cooked_turkey.json',{'variants':variants})

# The crafted feast keeps the roast's eight servings, on an original tray with garnish.
for name,base in [('feast_tray',(174,126,75)),('feast_carrot',(220,116,34)),('feast_greens',(75,115,42))]:
    im=Image.new('RGB',(16,16),base); px=im.load()
    for y in range(16):
        for x in range(16):
            delta=12 if (x+y*3)%7==0 else -10 if y%5==0 else 0
            px[x,y]=tuple(max(0,min(255,c+delta)) for c in base)
    im.save(block_dir/f'{name}.png')

def cuboid(low,high,texture):
    return {'from':low,'to':high,'faces':{side:{'texture':f'#{texture}'} for side in ('up','down','north','south','east','west')}}

for bite in range(8):
    roast=json.loads((ASSETS/'models/block'/f'cooked_turkey_{bite}.json').read_text())
    elements=copy.deepcopy(roast['elements'])
    for element in elements:
        element['from'][1]+=0.5; element['to'][1]+=0.5
    elements += [cuboid([0,0,0],[16,1,16],'tray'),
                 cuboid([0,1,0],[1,1.5,16],'tray'),cuboid([15,1,0],[16,1.5,16],'tray'),
                 cuboid([1,1,0],[15,1.5,1],'tray'),cuboid([1,1,15],[15,1.5,16],'tray')]
    garnish=[(x,z) for z in (1.5,13.5) for x in (2,5,8,11)]
    for x,z in garnish[bite:]:
        elements.append(cuboid([x,1,z],[x+1.5,1.7,z+1],'carrot'))
        elements.append(cuboid([x+1.5,1,z],[x+2,1.5,z+1],'greens'))
    textures=dict(roast['textures'],tray='turkeytime:block/feast_tray',
                  carrot='turkeytime:block/feast_carrot',greens='turkeytime:block/feast_greens')
    write(ASSETS/'models/block'/f'thanksgiving_feast_{bite}.json',{
        'parent':'minecraft:block/block','textures':textures,'elements':elements})

feast_variants={key:dict(value,model=value['model'].replace('cooked_turkey','thanksgiving_feast')) for key,value in variants.items()}
write(ASSETS/'blockstates/thanksgiving_feast.json',{'variants':feast_variants})
write(ASSETS/'items/thanksgiving_feast.json',{'model':{'type':'minecraft:model','model':'turkeytime:item/thanksgiving_feast'}})
write(ASSETS/'models/item/thanksgiving_feast.json',{'parent':'turkeytime:block/thanksgiving_feast_0'})
write(RES/'data/turkeytime/recipe/thanksgiving_feast.json',{
    'type':'minecraft:crafting_shaped','category':'misc',
    'pattern':['BTC','SSS'],
    'key':{'B':'minecraft:bread','T':'turkeytime:cooked_turkey','C':'minecraft:carrot','S':'#minecraft:wooden_slabs'},
    'result':{'id':'turkeytime:thanksgiving_feast','count':1}})
write(RES/'data/turkeytime/advancement/recipes/food/thanksgiving_feast.json',{
    'parent':'minecraft:recipes/root',
    'criteria':{'has_turkey':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'turkeytime:cooked_turkey'}]}},
                'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipes':'turkeytime:thanksgiving_feast'}}},
    'requirements':[['has_turkey','has_recipe']], 'rewards':{'recipes':['turkeytime:thanksgiving_feast']}})
write(RES/'data/turkeytime/loot_table/blocks/thanksgiving_feast.json',{
    'type':'minecraft:block','pools':[{'rolls':1,
      'condition':{'type':'minecraft:match_block','blocks':'turkeytime:thanksgiving_feast','state':{'bites':'0'}},
      'entries':[{'type':'minecraft:item','name':'turkeytime:thanksgiving_feast','modifier':[{'type':'minecraft:explosion_decay'}]}]}]})
print('Imported whole-turkey icons; generated eight clipped roast models, textures and recipe data.')
