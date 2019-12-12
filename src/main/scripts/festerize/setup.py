from setuptools import setup


setup (
    name='Festerize',
    version='0.1',
    py_modules=['festerize'],
    install_requires=[
        'click',
        'requests'
    ],
    entry_points='''
        [console_scripts]
        festerize=festerize:cli
    ''',
)
